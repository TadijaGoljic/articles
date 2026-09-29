// Security-rules tests for ../firestore.rules. Run with `npm test` (starts the Firestore emulator).
const fs = require("fs");
const path = require("path");
const {
  initializeTestEnvironment,
  assertSucceeds,
  assertFails,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc,
  collection,
  query,
  where,
  getDocs,
} = require("firebase/firestore");

const ALICE = "uid-alice";
const BOB = "uid-bob";
const EVE = "uid-eve";
const SHARED = "shared-1";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-articles",
    firestore: { rules: fs.readFileSync(path.join(__dirname, "..", "firestore.rules"), "utf8") },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  // Seed data with rules disabled: Alice owns a personal group and shares SHARED with Bob.
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await setDoc(doc(db, "groups/g-alice"), { idGroup: "g-alice", name: "Groceries", ownerUid: ALICE, isShared: false, updatedAt: 1 });
    await setDoc(doc(db, "articles/a-alice"), { idArticle: "a-alice", name: "Milk", ownerUid: ALICE, sharedGroupId: null, updatedAt: 1 });
    await setDoc(doc(db, "comments/c-alice"), { idComment: "c-alice", ownerUid: ALICE, sharedGroupId: null, updatedAt: 1 });
    await setDoc(doc(db, `sharedGroups/${SHARED}`), {
      sharedGroupId: SHARED, name: "Party", members: [ALICE, BOB], fromUid: ALICE, fromGroupId: "g-party", updatedAt: 1,
    });
    await setDoc(doc(db, "articles/a-shared"), { idArticle: "a-shared", name: "Chips", ownerUid: null, sharedGroupId: SHARED, updatedAt: 1 });
    await setDoc(doc(db, "comments/c-shared"), { idComment: "c-shared", ownerUid: null, sharedGroupId: SHARED, updatedAt: 1 });
    await setDoc(doc(db, `profiles/${BOB}`), { uid: BOB, email: "bob@example.com" });
    await setDoc(doc(db, "invitations/inv-1"), {
      invitationId: "inv-1", fromUid: ALICE, toUid: BOB, status: "PENDING", groupId: "g-alice",
    });
  });
});

const as = (uid) => testEnv.authenticatedContext(uid).firestore();
const anonymous = () => testEnv.unauthenticatedContext().firestore();

describe("personal data", () => {
  it("owners can read and write their own groups, articles and comments", async () => {
    const db = as(ALICE);
    await assertSucceeds(getDoc(doc(db, "groups/g-alice")));
    await assertSucceeds(getDoc(doc(db, "articles/a-alice")));
    await assertSucceeds(getDoc(doc(db, "comments/c-alice")));
    await assertSucceeds(updateDoc(doc(db, "articles/a-alice"), { bought: true }));
    await assertSucceeds(deleteDoc(doc(db, "groups/g-alice")));
  });

  it("other users cannot read or change someone else's personal data", async () => {
    const db = as(EVE);
    await assertFails(getDoc(doc(db, "groups/g-alice")));
    await assertFails(getDoc(doc(db, "articles/a-alice")));
    await assertFails(getDoc(doc(db, "comments/c-alice")));
    await assertFails(updateDoc(doc(db, "articles/a-alice"), { bought: true }));
    await assertFails(deleteDoc(doc(db, "groups/g-alice")));
  });

  it("signed-out clients can read nothing", async () => {
    const db = anonymous();
    await assertFails(getDoc(doc(db, "groups/g-alice")));
    await assertFails(getDoc(doc(db, `profiles/${BOB}`)));
  });

  it("new documents must be created with yourself as owner", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), "articles/a-new"), { name: "Eggs", ownerUid: ALICE, sharedGroupId: null }));
    await assertFails(setDoc(doc(as(EVE), "articles/a-forged"), { name: "Eggs", ownerUid: ALICE, sharedGroupId: null }));
    await assertFails(setDoc(doc(as(EVE), "groups/g-forged"), { name: "X", ownerUid: ALICE, isShared: false }));
  });

  it("owners can query their own articles, as sync does", async () => {
    const q = query(collection(as(ALICE), "articles"), where("ownerUid", "==", ALICE));
    await assertSucceeds(getDocs(q));
  });

  it("users cannot query everyone's articles", async () => {
    await assertFails(getDocs(collection(as(EVE), "articles")));
  });

  it("per-user order documents are private", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), `users/${ALICE}/groupOrder/g-alice`), { position: 1000 }));
    await assertFails(getDoc(doc(as(EVE), `users/${ALICE}/groupOrder/g-alice`)));
    await assertFails(setDoc(doc(as(EVE), `users/${ALICE}/articleOrder/a-alice`), { position: 1 }));
  });
});

describe("profiles", () => {
  it("any signed-in user can look a profile up by email, as sharing does", async () => {
    const q = query(collection(as(ALICE), "profiles"), where("email", "==", "bob@example.com"));
    const result = await assertSucceeds(getDocs(q));
    if (result.size !== 1) throw new Error(`expected one profile, got ${result.size}`);
  });

  it("only the owner can write or delete their profile", async () => {
    await assertSucceeds(setDoc(doc(as(BOB), `profiles/${BOB}`), { uid: BOB, email: "bob@example.com" }));
    await assertFails(setDoc(doc(as(EVE), `profiles/${BOB}`), { uid: BOB, email: "eve@example.com" }));
    await assertFails(deleteDoc(doc(as(EVE), `profiles/${BOB}`)));
    await assertSucceeds(deleteDoc(doc(as(BOB), `profiles/${BOB}`)));
  });
});

describe("invitations", () => {
  it("can only be sent in your own name", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), "invitations/inv-2"), { fromUid: ALICE, toUid: EVE, status: "PENDING" }));
    await assertFails(setDoc(doc(as(EVE), "invitations/inv-3"), { fromUid: ALICE, toUid: BOB, status: "PENDING" }));
  });

  it("are visible to sender and recipient only", async () => {
    await assertSucceeds(getDoc(doc(as(ALICE), "invitations/inv-1")));
    await assertSucceeds(getDoc(doc(as(BOB), "invitations/inv-1")));
    await assertFails(getDoc(doc(as(EVE), "invitations/inv-1")));
  });

  it("the recipient can accept, a stranger cannot", async () => {
    await assertSucceeds(updateDoc(doc(as(BOB), "invitations/inv-1"), { status: "ACCEPTED", sharedGroupId: SHARED }));
    await assertFails(updateDoc(doc(as(EVE), "invitations/inv-1"), { status: "ACCEPTED" }));
  });

  it("the pending-invitations query of the recipient is allowed", async () => {
    const q = query(
      collection(as(BOB), "invitations"),
      where("toUid", "==", BOB),
      where("status", "==", "PENDING"),
    );
    await assertSucceeds(getDocs(q));
  });
});

describe("shared groups", () => {
  it("members can read the group, strangers cannot", async () => {
    await assertSucceeds(getDoc(doc(as(BOB), `sharedGroups/${SHARED}`)));
    await assertFails(getDoc(doc(as(EVE), `sharedGroups/${SHARED}`)));
  });

  it("the owner can rename the group - the path used by the Edit dialog fix", async () => {
    await assertSucceeds(updateDoc(doc(as(ALICE), `sharedGroups/${SHARED}`), { name: "Shop" }));
  });

  it("the owner can archive and restore the group for everyone", async () => {
    await assertSucceeds(updateDoc(doc(as(ALICE), `sharedGroups/${SHARED}`), { isArchived: true, updatedAt: 2 }));
    await assertSucceeds(updateDoc(doc(as(ALICE), `sharedGroups/${SHARED}`), { isArchived: false, updatedAt: 3 }));
  });

  it("a member cannot archive someone else's group", async () => {
    await assertFails(updateDoc(doc(as(BOB), `sharedGroups/${SHARED}`), { isArchived: true, updatedAt: 2 }));
  });

  it("archiving resets the group's articles, which members may do", async () => {
    await assertSucceeds(updateDoc(doc(as(ALICE), "articles/a-shared"), { bought: false, updatedAt: 2 }));
  });

  it("a member cannot rename someone else's group", async () => {
    await assertFails(updateDoc(doc(as(BOB), `sharedGroups/${SHARED}`), { name: "Mine now" }));
  });

  it("a member can leave by removing only themselves", async () => {
    await assertSucceeds(updateDoc(doc(as(BOB), `sharedGroups/${SHARED}`), { members: [ALICE] }));
  });

  it("a member cannot remove somebody else or add people", async () => {
    await assertFails(updateDoc(doc(as(BOB), `sharedGroups/${SHARED}`), { members: [BOB] }));
    await assertFails(updateDoc(doc(as(BOB), `sharedGroups/${SHARED}`), { members: [ALICE, BOB, EVE] }));
  });

  it("strangers cannot join or change the group", async () => {
    await assertFails(updateDoc(doc(as(EVE), `sharedGroups/${SHARED}`), { members: [ALICE, BOB, EVE] }));
    await assertFails(deleteDoc(doc(as(EVE), `sharedGroups/${SHARED}`)));
  });

  it("members can list the groups they belong to, as sync does", async () => {
    const q = query(collection(as(BOB), "sharedGroups"), where("members", "array-contains", BOB));
    await assertSucceeds(getDocs(q));
  });

  it("members can read and edit the group's articles and comments", async () => {
    await assertSucceeds(getDoc(doc(as(BOB), "articles/a-shared")));
    await assertSucceeds(updateDoc(doc(as(BOB), "articles/a-shared"), { bought: true, assignedToUid: BOB }));
    await assertSucceeds(getDoc(doc(as(BOB), "comments/c-shared")));
    await assertSucceeds(
      setDoc(doc(as(BOB), "articles/a-bob"), { name: "Dip", ownerUid: null, sharedGroupId: SHARED }),
    );
  });

  it("strangers cannot read or add to a shared group's articles", async () => {
    await assertFails(getDoc(doc(as(EVE), "articles/a-shared")));
    await assertFails(getDoc(doc(as(EVE), "comments/c-shared")));
    await assertFails(setDoc(doc(as(EVE), "articles/a-eve"), { name: "Spam", ownerUid: null, sharedGroupId: SHARED }));
  });

  it("members can query the shared articles, as sync does", async () => {
    const q = query(collection(as(BOB), "articles"), where("sharedGroupId", "==", SHARED));
    await assertSucceeds(getDocs(q));
  });

  it("after leaving, a former member loses access", async () => {
    await testEnv.withSecurityRulesDisabled((context) =>
      updateDoc(doc(context.firestore(), `sharedGroups/${SHARED}`), { members: [ALICE] }),
    );
    await assertFails(getDoc(doc(as(BOB), "articles/a-shared")));
    await assertFails(getDoc(doc(as(BOB), `sharedGroups/${SHARED}`)));
  });
});

describe("account deletion cleanup", () => {
  // Mirrors AccountRepository.deleteUserData: everything it deletes must be allowed while signed in.
  it("a user can delete all of their own data", async () => {
    await testEnv.withSecurityRulesDisabled((context) =>
      setDoc(doc(context.firestore(), `users/${ALICE}`), { fcmTokens: { device: "token" } }),
    );
    const db = as(ALICE);
    await assertSucceeds(deleteDoc(doc(db, "articles/a-alice")));
    await assertSucceeds(deleteDoc(doc(db, "comments/c-alice")));
    await assertSucceeds(deleteDoc(doc(db, "groups/g-alice")));
    await assertSucceeds(deleteDoc(doc(db, `profiles/${ALICE}`)));
    await assertSucceeds(deleteDoc(doc(db, `users/${ALICE}`)));
  });

  it("the owner can delete a shared group with its articles and comments", async () => {
    const db = as(ALICE);
    await assertSucceeds(deleteDoc(doc(db, "articles/a-shared")));
    await assertSucceeds(deleteDoc(doc(db, "comments/c-shared")));
    await assertSucceeds(deleteDoc(doc(db, `sharedGroups/${SHARED}`)));
  });
});
