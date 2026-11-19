// Security rules tests. Run from firebase/ (needs Java and firebase-tools):
//   npm --prefix rules-test install
//   firebase emulators:exec --only firestore "npm --prefix rules-test test"
import { readFileSync } from "node:fs";
import { after, before, beforeEach, describe, it } from "node:test";
import { assertFails, assertSucceeds, initializeTestEnvironment } from "@firebase/rules-unit-testing";
import { doc, getDoc, serverTimestamp, setDoc, updateDoc, deleteDoc } from "firebase/firestore";

let env;
const ALICE = "alice";
const BOB = "bob";

before(async () => {
  env = await initializeTestEnvironment({
    projectId: "track14-rules-test",
    firestore: { rules: readFileSync(new URL("../firestore.rules", import.meta.url), "utf8") },
  });
});
after(() => env.cleanup());
beforeEach(() => env.clearFirestore());

const as = (uid, claims = {}) => env.authenticatedContext(uid, claims).firestore();
const anon = () => env.unauthenticatedContext().firestore();
const seed = (fn) => env.withSecurityRulesDisabled((ctx) => fn(ctx.firestore()));

const profile = (extra = {}) => ({
  displayName: "Alice",
  photoUrl: null,
  email: "alice@example.com",
  lang: "uz",
  timezone: "Asia/Tashkent",
  deviceModel: "Pixel 8",
  usageAccess: false,
  createdAt: serverTimestamp(),
  ...extra,
});

describe("users", () => {
  it("creates only its own valid profile", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), "users", ALICE), profile()));
    await assertFails(setDoc(doc(as(BOB), "users", ALICE), profile()));
    await assertFails(setDoc(doc(as(ALICE), "users", ALICE), profile({ blocked: false })));
    await assertFails(setDoc(doc(as(ALICE), "users", ALICE), profile({ displayName: "x".repeat(101) })));
  });

  it("cannot touch server fields", async () => {
    await seed((db) => setDoc(doc(db, "users", ALICE), { displayName: "Alice", stats: {}, blocked: false }));
    await assertSucceeds(updateDoc(doc(as(ALICE), "users", ALICE), { lang: "ru" }));
    await assertFails(updateDoc(doc(as(ALICE), "users", ALICE), { blocked: false, lang: "en" }));
    await assertFails(updateDoc(doc(as(ALICE), "users", ALICE), { "stats.testsCompleted": 99 }));
  });

  it("blocked users cannot edit their profile", async () => {
    await seed((db) => setDoc(doc(db, "users", ALICE), { displayName: "Alice", blocked: true }));
    await assertFails(updateDoc(doc(as(ALICE), "users", ALICE), { lang: "ru" }));
  });

  it("others cannot read a profile, staff can", async () => {
    await seed((db) => setDoc(doc(db, "users", ALICE), { displayName: "Alice" }));
    await assertFails(getDoc(doc(as(BOB), "users", ALICE)));
    await assertSucceeds(getDoc(doc(as(BOB, { role: "support" }), "users", ALICE)));
    await assertFails(getDoc(doc(as(BOB, { role: "admin" }), "users", ALICE)));
  });
});

describe("money and server-only collections", () => {
  it("nobody writes wallets, transactions, tests, campaigns, catalog or config from a client", async () => {
    const superadmin = as(BOB, { role: "superadmin" });
    for (const db of [as(ALICE), superadmin]) {
      await assertFails(setDoc(doc(db, "wallets", ALICE), { balance: 1e9 }));
      await assertFails(setDoc(doc(db, "transactions", "t1"), { uid: ALICE, amount: 1e9 }));
      await assertFails(setDoc(doc(db, "tests", "t1"), { testerUid: ALICE }));
      await assertFails(setDoc(doc(db, "campaigns", "a1"), { ownerUid: ALICE }));
      await assertFails(setDoc(doc(db, "catalog", "a1"), { name: "x" }));
      await assertFails(setDoc(doc(db, "config", "economy"), { welcomeBonus: 1e9 }));
      await assertFails(setDoc(doc(db, "auditLogs", "x"), { action: "fake" }));
    }
  });

  it("reads only its own transactions", async () => {
    await seed((db) => setDoc(doc(db, "transactions", "t1"), { uid: ALICE, amount: 3 }));
    await assertSucceeds(getDoc(doc(as(ALICE), "transactions", "t1")));
    await assertFails(getDoc(doc(as(BOB), "transactions", "t1")));
  });

  it("catalog and config are public to read", async () => {
    await seed((db) => setDoc(doc(db, "config", "flags"), { maintenance: false }));
    await assertSucceeds(getDoc(doc(anon(), "config", "flags")));
    await assertSucceeds(getDoc(doc(anon(), "catalog", "a1")));
  });
});

describe("apps", () => {
  const draft = (extra = {}) => ({
    ownerUid: ALICE,
    status: "draft",
    createdAt: serverTimestamp(),
    name: "My app",
    packageName: "uz.alice.app",
    optInUrl: "https://play.google.com/apps/testing/uz.alice.app",
    ...extra,
  });

  it("owner creates a draft with a valid package and opt-in link", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), "apps", "a1"), draft()));
    await assertFails(setDoc(doc(as(ALICE), "apps", "a2"), draft({ status: "active" })));
    await assertFails(setDoc(doc(as(ALICE), "apps", "a3"), draft({ ownerUid: BOB })));
    await assertFails(setDoc(doc(as(ALICE), "apps", "a4"), draft({ packageName: "../evil" })));
    await assertFails(setDoc(doc(as(ALICE), "apps", "a5"), draft({ optInUrl: "https://evil.io/x" })));
  });

  it("owner cannot change status", async () => {
    await seed((db) => setDoc(doc(db, "apps", "a1"), { ownerUid: ALICE, status: "draft", name: "x" }));
    await assertSucceeds(updateDoc(doc(as(ALICE), "apps", "a1"), { name: "y" }));
    await assertFails(updateDoc(doc(as(ALICE), "apps", "a1"), { status: "published" }));
    await assertFails(updateDoc(doc(as(BOB), "apps", "a1"), { name: "z" }));
    await assertFails(deleteDoc(doc(as(ALICE), "apps", "a1")));
  });
});

describe("feedback", () => {
  beforeEach(() => seed((db) => setDoc(doc(db, "tests", "t1"), { testerUid: ALICE, ownerUid: BOB, appId: "a1" })));
  const fb = (extra = {}) => ({
    testId: "t1",
    appId: "a1",
    testerUid: ALICE,
    ownerUid: BOB,
    rating: 5,
    tags: ["bug"],
    text: "Works",
    createdAt: serverTimestamp(),
    ...extra,
  });

  it("only the tester of that test, with the test's own owner and app", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), "feedback", "f1"), fb()));
    await assertFails(setDoc(doc(as(BOB), "feedback", "f2"), fb({ testerUid: BOB })));
    await assertFails(setDoc(doc(as(ALICE), "feedback", "f3"), fb({ ownerUid: "someone" })));
    await assertFails(setDoc(doc(as(ALICE), "feedback", "f4"), fb({ rating: 9 })));
    await assertFails(setDoc(doc(as(ALICE), "feedback", "f5"), fb({ tags: ["<script>"] })));
  });
});

describe("reports", () => {
  const report = { reporterUid: ALICE, targetType: "app", targetId: "a1", reason: "spam", status: "open", createdAt: serverTimestamp() };

  it("users file open reports; nobody edits them from a client", async () => {
    await assertSucceeds(setDoc(doc(as(ALICE), "reports", "r1"), report));
    await assertFails(setDoc(doc(as(ALICE), "reports", "r2"), { ...report, status: "resolved" }));
    await assertFails(updateDoc(doc(as(BOB, { role: "moderator" }), "reports", "r1"), { status: "resolved" }));
  });
});

describe("staff collections", () => {
  it("push jobs and audit logs are staff-read only", async () => {
    await assertFails(getDoc(doc(as(ALICE), "auditLogs", "x")));
    await assertSucceeds(getDoc(doc(as(ALICE, { role: "moderator" }), "auditLogs", "x")));
    await assertFails(getDoc(doc(as(ALICE), "pushJobs", "x")));
  });
});
