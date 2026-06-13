const { onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onArticleChanged = onDocumentWritten(
  "users/{uid}/articles/{articleId}",
  async (event) => {
    await notifyDevices(event.params.uid);
  }
);

exports.onGroupChanged = onDocumentWritten(
  "users/{uid}/groups/{groupId}",
  async (event) => {
    await notifyDevices(event.params.uid);
  }
);

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
