const {setGlobalOptions} = require("firebase-functions");
const {onRequest} = require("firebase-functions/https");
const {onDocumentWritten, onDocumentDeleted} = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const admin = require("firebase-admin");

admin.initializeApp();

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
setGlobalOptions({ maxInstances: 10 });

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
