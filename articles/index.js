const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const {onDocumentWritten, onDocumentCreated, onDocumentDeleted} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

setGlobalOptions({ maxInstances: 10 });

exports.onArticleChanged = onDocumentWritten(
  "articles/{articleId}",
  async (event) => {
    await notifyForOwnedOrSharedDocument(event.data?.after?.data() ?? event.data?.before?.data());
  }
);

exports.onCommentChanged = onDocumentWritten(
  "comments/{commentId}",
  async (event) => {
    await notifyForOwnedOrSharedDocument(event.data?.after?.data() ?? event.data?.before?.data());
  }
);

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
    const members = new Set([
      ...(event.data?.before?.data()?.members || []),
      ...(event.data?.after?.data()?.members || []),
    ]);
    if (members.size === 0) return;
    await Promise.all([...members].map((uid) => notifyDevices(uid)));
  }
);

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
  if (data.ownerUid) {
    await notifyDevices(data.ownerUid);
    return;
  }
  if (data.sharedGroupId) {
    const groupDoc = await admin.firestore().collection("sharedGroups").doc(data.sharedGroupId).get();
    const members = groupDoc.data()?.members;
    if (!Array.isArray(members)) return;
    await Promise.all(members.map((uid) => notifyDevices(uid)));
  }
}

async function notifyDevices(uid) {
  const userDoc = await admin.firestore().collection("users").doc(uid).get();
  const fcmTokens = userDoc.data()?.fcmTokens;
  if (!fcmTokens) return;

  const tokens = Object.values(fcmTokens).filter(Boolean);
  if (tokens.length === 0) return;

  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    android: { priority: "high" },
    data: { type: "sync" },
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
