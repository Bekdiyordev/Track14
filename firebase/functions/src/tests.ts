import { DocumentReference, FieldValue, getFirestore, Timestamp, Transaction } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { z } from "zod";
import { isValidPackageName } from "./catalog";
import { loadEconomy } from "./economy";
import { messages, notifySafely } from "./notify";
import { docId, parse, requireActiveUser } from "./guards";
import { applyEntry, readEntry } from "./ledger";
import { bumpStat, reliability } from "./stats";
import { callableOptions } from "./options";
import { dayNumber, isPassed, isValidTimeZone, ledgerKey, MIN_FOREGROUND_SECONDS, MIN_PASSED_TO_COMPLETE, TEST_DAYS } from "./rules";

const db = () => getFirestore();
const testRef = (id: string) => db().collection("tests").doc(id);
const campaignRef = (id: string) => db().collection("campaigns").doc(id);

/**
 * What the owner may see about a tester (privacy policy: name, device, reliability):
 * first name plus the initial of the last name, never the e-mail or uid.
 */
export function shortName(displayName: unknown): string {
  const parts = String(displayName ?? "").trim().split(/\s+/).filter(Boolean);
  if (parts.length === 0) return "";
  const first = parts[0].slice(0, 30);
  return parts.length > 1 ? `${first} ${parts[parts.length - 1].charAt(0).toUpperCase()}.` : first;
}

/** Snapshot copied onto the test at join time; the owner can read tests but not users. */
export function testerSnapshot(user: { get(field: string): unknown }): Record<string, unknown> {
  const stats = (user.get("stats") ?? {}) as Record<string, number>;
  const model = user.get("deviceModel");
  return {
    testerName: shortName(user.get("displayName")),
    testerDevice: typeof model === "string" ? model.slice(0, 60) : "",
    testerReliability: reliability(stats),
    testerCompleted: Number(stats.testsCompleted ?? 0),
  };
}

/** Deterministic id: one test per tester per campaign, so joining twice is a no-op. */
export const testIdOf = (campaignId: string, uid: string) => `${campaignId}_${uid}`;

const StartTest = z
  .object({
    campaignId: docId,
    timezone: z.string().min(1).max(64).refine(isValidTimeZone),
  })
  .strict();

export const startTest = onCall(callableOptions, async (req) => {
  const uid = await requireActiveUser(req);
  const { campaignId, timezone } = parse(StartTest, req.data);
  const economy = await loadEconomy();
  const testId = testIdOf(campaignId, uid);

  const active = await db()
    .collection("tests")
    .where("testerUid", "==", uid)
    .where("status", "==", "active")
    .count()
    .get();

  const result = await db().runTransaction(async (tx) => {
    const [campaign, test, app, user] = await Promise.all([
      tx.get(campaignRef(campaignId)),
      tx.get(testRef(testId)),
      tx.get(db().collection("apps").doc(campaignId)),
      tx.get(db().collection("users").doc(uid)),
    ]);
    if (test.exists) return { testId, status: test.get("status") as string, joined: null };
    if (!campaign.exists || campaign.get("status") !== "active") {
      throw new HttpsError("failed-precondition", "campaign_not_active");
    }
    if (campaign.get("ownerUid") === uid) throw new HttpsError("failed-precondition", "own_app");
    if (active.data().count >= economy.maxActiveTests) {
      throw new HttpsError("resource-exhausted", "max_active_tests");
    }

    tx.set(testRef(testId), {
      campaignId,
      appId: campaign.get("appId"),
      appName: String(app.get("name") ?? "").slice(0, 60),
      packageName: isValidPackageName(app.get("packageName")) ? app.get("packageName") : "",
      testerUid: uid,
      ownerUid: campaign.get("ownerUid"),
      status: "active",
      rewardPerDay: campaign.get("rewardPerDay"),
      completionBonus: campaign.get("completionBonus"),
      timezone,
      startedAt: FieldValue.serverTimestamp(),
      currentDay: 0,
      daysPassed: 0,
      daysMissed: 0,
      missedInRow: 0,
      lastPassedAt: null,
      feedbackRewarded: false,
      ...testerSnapshot(user),
    });
    tx.update(campaignRef(campaignId), { "stats.joined": FieldValue.increment(1) });
    bumpStat(tx, uid, "testsStarted");
    return {
      testId,
      status: "active",
      joined: {
        owner: String(campaign.get("ownerUid")),
        app: String(app.get("name") ?? ""),
        count: Number(campaign.get("stats.joined") ?? 0) + 1,
        needed: Number(campaign.get("testersNeeded") ?? 12),
      },
    };
  });
  if (result.joined) {
    const j = result.joined;
    await notifySafely(j.owner, { ...messages.testerJoined(j.app, j.count, j.needed), refId: campaignId });
  }
  return { testId: result.testId, status: result.status };
});

const VerifyDay = z
  .object({
    testId: z.string().regex(/^[A-Za-z0-9_-]{1,160}$/),
    seconds: z.number().int().min(0).max(86_400),
    mode: z.enum(["usage", "fallback"]),
  })
  .strict();

/**
 * Records today's check. The day number comes from the server clock and the test's
 * time zone, never from the client, so a tester cannot fill past or future days.
 */
export const verifyDay = onCall(callableOptions, async (req) => {
  const uid = await requireActiveUser(req);
  const { testId, seconds, mode } = parse(VerifyDay, req.data);

  return db().runTransaction(async (tx) => {
    const test = await tx.get(testRef(testId));
    if (!test.exists || test.get("testerUid") !== uid) throw new HttpsError("not-found", "test_not_found");
    if (test.get("status") !== "active") throw new HttpsError("failed-precondition", "test_not_active");

    const startedAt = test.get("startedAt") as Timestamp | null;
    if (!startedAt) throw new HttpsError("unavailable", "test_starting");
    const day = dayNumber(startedAt.toMillis(), Date.now(), test.get("timezone"));
    if (day < 1 || day > TEST_DAYS) throw new HttpsError("failed-precondition", "outside_test_window");

    const dayRef = test.ref.collection("days").doc(String(day));
    const rewardKey = ledgerKey(testId, day);
    const bonusKey = ledgerKey(testId, "bonus");
    const [dayDoc, rewardEntry, bonusEntry] = await Promise.all([
      tx.get(dayRef),
      readEntry(tx, rewardKey),
      readEntry(tx, bonusKey),
    ]);

    if (dayDoc.get("status") === "passed") return { day, passed: true, alreadyRecorded: true };
    if (!isPassed(seconds)) return { day, passed: false, minSeconds: MIN_FOREGROUND_SECONDS };

    const reward = Number(test.get("rewardPerDay") ?? 0);
    tx.set(dayRef, {
      n: day,
      status: "passed",
      seconds,
      mode,
      verifiedAt: FieldValue.serverTimestamp(),
    });
    tx.update(test.ref, {
      daysPassed: FieldValue.increment(1),
      missedInRow: 0,
      currentDay: day,
      lastPassedAt: FieldValue.serverTimestamp(),
    });
    applyEntry(tx, rewardEntry, { key: rewardKey, uid, amount: reward, type: "earn_day", refId: testId });
    bumpStat(tx, uid, "daysPassed");

    const passedTotal = Number(test.get("daysPassed") ?? 0) + 1;
    let completed = false;
    if (day === TEST_DAYS && passedTotal >= MIN_PASSED_TO_COMPLETE) {
      completed = true;
      completeTest(tx, test.ref, test.get("campaignId"), bonusEntry, {
        key: bonusKey,
        uid,
        amount: Number(test.get("completionBonus") ?? 0),
        testId,
      });
    }
    return { day, passed: true, reward, completed };
  });
});

/** Shared by verifyDay and the rollover: same bonus key, so the bonus is paid once. */
export function completeTest(
  tx: Transaction,
  ref: DocumentReference,
  campaignId: string,
  bonusEntry: FirebaseFirestore.DocumentSnapshot,
  bonus: { key: string; uid: string; amount: number; testId: string },
): void {
  tx.update(ref, { status: "completed", completedAt: FieldValue.serverTimestamp() });
  tx.update(campaignRef(campaignId), { "stats.completed14": FieldValue.increment(1) });
  bumpStat(tx, bonus.uid, "testsCompleted");
  applyEntry(tx, bonusEntry, { key: bonus.key, uid: bonus.uid, amount: bonus.amount, type: "bonus", refId: bonus.testId });
}

const LeaveTest = z.object({ testId: z.string().regex(/^[A-Za-z0-9_-]{1,160}$/) }).strict();

/** Leaving an active test costs the "left" penalty (plan, section 5). */
export const leaveTest = onCall(callableOptions, async (req) => {
  const uid = await requireActiveUser(req);
  const { testId } = parse(LeaveTest, req.data);
  const economy = await loadEconomy();
  const key = ledgerKey(testId, "left");

  return db().runTransaction(async (tx) => {
    const [test, entry] = await Promise.all([tx.get(testRef(testId)), readEntry(tx, key)]);
    if (!test.exists || test.get("testerUid") !== uid) throw new HttpsError("not-found", "test_not_found");
    if (test.get("status") !== "active") return { status: test.get("status") as string };

    tx.update(test.ref, { status: "left", endedAt: FieldValue.serverTimestamp() });
    tx.update(campaignRef(test.get("campaignId")), { "stats.left": FieldValue.increment(1) });
    bumpStat(tx, uid, "testsLeft");
    applyEntry(tx, entry, { key, uid, amount: -economy.leftPenalty, type: "penalty", refId: testId });
    return { status: "left", penalty: economy.leftPenalty };
  });
});
