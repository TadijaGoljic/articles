const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const {
  onDocumentWritten,
  onDocumentWrittenWithAuthContext,
  onDocumentCreated,
  onDocumentDeleted,
} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

exports.onArticleChanged = onDocumentWrittenWithAuthContext(
  "articles/{articleId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    await notifyForOwnedOrSharedDocument(after ?? before);

    const activity = describeArticleActivity(before, after);
    const sharedGroupId = after?.sharedGroupId ?? before?.sharedGroupId;
    if (!activity || !sharedGroupId) return;

    const actorEmail = await getActorEmail(event.authId);
    if (!actorEmail) return;

    await sendGroupActivityNotification(
      sharedGroupId,
      event.authId,
      formatArticleActivityMessage(actorEmail, activity)
    );
  }
);

exports.onCommentChanged = onDocumentWrittenWithAuthContext(
  "comments/{commentId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    await notifyForOwnedOrSharedDocument(after ?? before);

    if (before || !after || !after.sharedGroupId) return;

    const actorEmail = await getActorEmail(event.authId);
    if (!actorEmail) return;

    const articleName = after.articleId ? await getArticleName(after.articleId) : null;
    const message = formatCommentActivityMessage(actorEmail, articleName, after.text || "");

    await sendGroupActivityNotification(after.sharedGroupId, event.authId, message);
  }
);

function describeArticleActivity(before, after) {
  const name = (after ?? before)?.name || "an article";
  if (!before && after) return { action: "added", name };
  if (before && !after) return { action: "removed", name };
  if (before && after && before.bought !== after.bought) {
    return { action: after.bought ? "bought" : "unbought", name };
  }
  return null;
}

function formatArticleActivityMessage(actorEmail, { action, name }) {
  switch (action) {
    case "added":
      return `${actorEmail} added "${name}"`;
    case "removed":
      return `${actorEmail} removed "${name}"`;
    case "bought":
      return `${actorEmail} marked "${name}" as bought`;
    case "unbought":
      return `${actorEmail} marked "${name}" as not bought`;
    default:
      return null;
  }
}

function formatCommentActivityMessage(actorEmail, articleName, text) {
  const truncatedText = truncate(text, 120);
  return articleName
    ? `${actorEmail} commented on "${articleName}": ${truncatedText}`
    : `${actorEmail} commented: ${truncatedText}`;
}

function truncate(text, maxLength) {
  return text.length > maxLength ? `${text.slice(0, maxLength - 1)}…` : text;
}

async function getActorEmail(authId) {
  if (!authId) return null;
  const doc = await admin.firestore().collection("profiles").doc(authId).get();
  return doc.data()?.email ?? null;
}

async function getArticleName(articleId) {
  const doc = await admin.firestore().collection("articles").doc(articleId).get();
  return doc.data()?.name ?? null;
}

async function sendGroupActivityNotification(sharedGroupId, actorUid, body) {
  if (!body) return;
  const groupDoc = await admin.firestore().collection("sharedGroups").doc(sharedGroupId).get();
  const groupData = groupDoc.data();
  const members = Array.isArray(groupData?.members) ? groupData.members : [];
  const recipients = members.filter((uid) => uid !== actorUid);
  if (recipients.length === 0) return;

  await Promise.all(
    recipients.map((uid) =>
      notifyDevices(uid, {
        type: "groupActivity",
        groupId: sharedGroupId,
        title: groupData?.name || "",
        body,
      })
    )
  );
}

exports.onGroupChanged = onDocumentWritten(
  "groups/{groupId}",
  async (event) => {
    const ownerUid = event.data?.after?.data()?.ownerUid ?? event.data?.before?.data()?.ownerUid;
    if (!ownerUid) return;
    await notifyDevices(ownerUid);
  }
);

exports.onSharedGroupChanged = onDocumentWritten(
  "sharedGroups/{groupId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    const members = new Set([
      ...(before?.members || []),
      ...(after?.members || []),
    ]);

    if (
      after &&
      Array.isArray(after.members) &&
      after.members.length === 1 &&
      after.members.includes(after.fromUid) &&
      after.fromGroupId
    ) {
      await revertSharedGroupToPersonal(event.params.groupId, after);
      if (members.size > 0) {
        await Promise.all([...members].map((uid) => notifyDevices(uid)));
      }
      return;
    }

    if (members.size > 0) {
      await Promise.all([...members].map((uid) => notifyDevices(uid)));
    }
  }
);

async function revertSharedGroupToPersonal(sharedGroupId, sharedGroupData) {
  const { fromUid, fromGroupId, name } = sharedGroupData;

  await Promise.all([
    untagCollectionAsShared("articles", sharedGroupId, fromUid),
    untagCollectionAsShared("comments", sharedGroupId, fromUid),
  ]);
  const batch = admin.firestore().batch();
  batch.set(admin.firestore().collection("groups").doc(fromGroupId), {
    idGroup: fromGroupId,
    name: name || "",
    updatedAt: Date.now(),
    isShared: false,
    sharedGroupId: null,
    ownerUid: fromUid,
    isArchived: sharedGroupData.isArchived === true,
  });
  batch.delete(admin.firestore().collection("sharedGroups").doc(sharedGroupId));
  await batch.commit();
}

async function untagCollectionAsShared(collectionName, sharedGroupId, ownerUid) {
  const snapshot = await admin.firestore()
    .collection(collectionName)
    .where("sharedGroupId", "==", sharedGroupId)
    .get();
  if (snapshot.empty) return;

  const BATCH_LIMIT = 450;
  for (let i = 0; i < snapshot.docs.length; i += BATCH_LIMIT) {
    const batch = admin.firestore().batch();
    snapshot.docs.slice(i, i + BATCH_LIMIT).forEach((doc) => {
      batch.update(doc.ref, {
        sharedGroupId: admin.firestore.FieldValue.delete(),
        ownerUid,
      });
    });
    await batch.commit();
  }
}

exports.onInvitationAccepted = onDocumentWritten(
  "invitations/{invitationId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!after || after.status !== "ACCEPTED") return;
    if (before?.status === "ACCEPTED") return;
    if (!after.sharedGroupId) return;

    const sharedGroupRef = admin.firestore().collection("sharedGroups").doc(after.sharedGroupId);

    if (after.existingSharedGroupId) {
      await sharedGroupRef.update({
        members: admin.firestore.FieldValue.arrayUnion(after.toUid),
      });
    } else {
      if (!after.groupId) return;
      await Promise.all([
        tagCollectionAsShared("articles", after.groupId, after.sharedGroupId),
        tagCollectionAsShared("comments", after.groupId, after.sharedGroupId),
        admin.firestore().collection("groups").doc(after.groupId).delete(),
      ]);
    }

    const sharedGroupDoc = await sharedGroupRef.get();
    const members = sharedGroupDoc.data()?.members;
    if (Array.isArray(members)) {
      await Promise.all(members.map((uid) => notifyDevices(uid)));
    }
  }
);

async function tagCollectionAsShared(collectionName, groupId, sharedGroupId) {
  const snapshot = await admin.firestore()
    .collection(collectionName)
    .where("groupId", "==", groupId)
    .get();
  if (snapshot.empty) return;

  const docsToTag = snapshot.docs.filter((doc) => doc.data().sharedGroupId !== sharedGroupId);
  if (docsToTag.length === 0) return;

  const BATCH_LIMIT = 450;
  for (let i = 0; i < docsToTag.length; i += BATCH_LIMIT) {
    const batch = admin.firestore().batch();
    docsToTag.slice(i, i + BATCH_LIMIT).forEach((doc) => {
      batch.update(doc.ref, { sharedGroupId });
    });
    await batch.commit();
  }
}

exports.onInvitationCreated = onDocumentCreated(
  "invitations/{invitationId}",
  async (event) => {
    const invitation = event.data?.data();
    if (!invitation || invitation.status !== "PENDING") return;

    const userDoc = await admin.firestore()
      .collection("users")
      .doc(invitation.toUid)
      .get();
    const fcmTokens = userDoc.data()?.fcmTokens;
    if (!fcmTokens) return;

    const tokens = Object.values(fcmTokens).filter(Boolean);
    if (tokens.length === 0) return;

    await admin.messaging().sendEachForMulticast({
      tokens,
      android: { priority: "high" },
      data: {
        type: "invitation",
        invitationId: event.params.invitationId,
        fromEmail: invitation.fromEmail || "",
        groupName: invitation.groupName || "",
      },
    });
  }
);

exports.onUserDeleted = onDocumentDeleted(
  "users/{uid}",
  async (event) => {
    const fcmTokens = event.data?.data()?.fcmTokens;
    if (!fcmTokens) return;

    const tokens = Object.values(fcmTokens).filter(Boolean);
    if (tokens.length === 0) return;

    await admin.messaging().sendEachForMulticast({
      tokens,
      android: { priority: "high" },
      data: { type: "forceSignOut" },
    });
  }
);

async function notifyForOwnedOrSharedDocument(data) {
  if (!data) return;
  const uidsToNotify = new Set();
  if (data.ownerUid) uidsToNotify.add(data.ownerUid);
  if (data.sharedGroupId) {
    const groupDoc = await admin.firestore().collection("sharedGroups").doc(data.sharedGroupId).get();
    const members = groupDoc.data()?.members;
    if (Array.isArray(members)) members.forEach((uid) => uidsToNotify.add(uid));
  }
  await Promise.all([...uidsToNotify].map((uid) => notifyDevices(uid)));
}

async function notifyDevices(uid, data = { type: "sync" }) {
  const userDoc = await admin.firestore().collection("users").doc(uid).get();
  const fcmTokens = userDoc.data()?.fcmTokens;
  if (!fcmTokens) return;

  const tokens = Object.values(fcmTokens).filter(Boolean);
  if (tokens.length === 0) return;

  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    android: { priority: "high" },
    data,
  });

  const staleTokens = [];
  response.responses.forEach((res, i) => {
    if (!res.success && res.error?.code === "messaging/registration-token-not-registered") {
      staleTokens.push(tokens[i]);
    }
  });

  if (staleTokens.length > 0) {
    const updates = {};
    const tokenEntries = Object.entries(fcmTokens);
    staleTokens.forEach((stale) => {
      const entry = tokenEntries.find(([, v]) => v === stale);
      if (entry) updates[`fcmTokens.${entry[0]}`] = admin.firestore.FieldValue.delete();
    });
    await admin.firestore().collection("users").doc(uid).update(updates);
  }
}
