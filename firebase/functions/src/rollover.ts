import { FieldPath, FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions/v2";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { loadEconomy } from "./economy";
import { applyEntry, readEntry } from "./ledger";
import { REGION } from "./options";
import { closedDayStatuses, dayNumber, evaluate, ledgerKey } from "./rules";
import { completeTest } from "./tests";
import { bumpStat } from "./stats";
import { messages, notifySafely } from "./notify";

const PAGE = 200;

/**
 * Runs hourly because testers live in different time zones. For every active test it
 * closes the days that are over: unchecked days become "missed", then the streak rules
 * decide whether the test continues, completes or fails (with the penalty).
 */
export const dailyRollover = onSchedule(
  { schedule: "every 60 minutes", region: REGION, timeoutSeconds: 540, memory: "512MiB" },
  async () => {
    const db = getFirestore();
    const economy = await loadEconomy();
    let last: string | undefined;
    let processed = 0;
    for (;;) {
      let query = db.collection("tests").where("status", "==", "active").orderBy(FieldPath.documentId()).limit(PAGE);
      if (last) query = query.startAfter(last);
      const page = await query.get();
      if (page.empty) break;
      for (const doc of page.docs) {
        try {
          await closeDays(doc.id, economy.failedPenalty);
        } catch (e) {
          logger.error("rollover failed", { testId: doc.id, error: String(e) });
        }
      }
      processed += page.size;
      last = page.docs[page.docs.length - 1].id;
      if (page.size < PAGE) break;
    }
    logger.info("rollover done", { processed });
  },
);

async function closeDays(testId: string, failedPenalty: number): Promise<void> {
  const db = getFirestore();
  const result = await db.runTransaction(async (tx): Promise<{ uid: string; app: string; outcome: string; bonus: number } | null> => {
    const ref = db.collection("tests").doc(testId);
    const test = await tx.get(ref);
    if (test.get("status") !== "active") return null;
    const startedAt = test.get("startedAt") as Timestamp | null;
    if (!startedAt) return null;

    const tester = await tx.get(db.collection("users").doc(String(test.get("testerUid"))));
    if (tester.get("blocked") === true) return null;

    const today = dayNumber(startedAt.toMillis(), Date.now(), test.get("timezone"));
    const closedUpTo = today - 1;
    if (closedUpTo < 1) return null;

    const days = await tx.get(ref.collection("days"));
    const passed = new Set<number>();
    const recorded = new Set<number>();
    for (const d of days.docs) {
      const n = Number(d.id);
      recorded.add(n);
      if (d.get("status") === "passed") passed.add(n);
    }
    const statuses = closedDayStatuses(passed, closedUpTo);
    const outcome = evaluate(statuses);

    const uid = test.get("testerUid") as string;
    const campaignId = test.get("campaignId") as string;
    const failKey = ledgerKey(testId, "failed");
    const bonusKey = ledgerKey(testId, "bonus");
    const [failEntry, bonusEntry] = await Promise.all([readEntry(tx, failKey), readEntry(tx, bonusKey)]);

    // Writes start here.
    let missedTotal = 0;
    let missedInRow = 0;
    statuses.forEach((s, i) => {
      const n = i + 1;
      if (s === "missed") {
        missedTotal++;
        missedInRow++;
        if (!recorded.has(n)) {
          tx.set(ref.collection("days").doc(String(n)), { n, status: "missed", closedAt: FieldValue.serverTimestamp() });
        }
      } else {
        missedInRow = 0;
      }
    });
    tx.update(ref, { daysMissed: missedTotal, missedInRow, currentDay: Math.min(today, 14) });

    if (outcome === "failed") {
      tx.update(ref, { status: "failed", endedAt: FieldValue.serverTimestamp() });
      tx.update(db.collection("campaigns").doc(campaignId), { "stats.failed": FieldValue.increment(1) });
      bumpStat(tx, uid, "testsFailed");
      applyEntry(tx, failEntry, { key: failKey, uid, amount: -failedPenalty, type: "penalty", refId: testId });
    } else if (outcome === "completed") {
      completeTest(tx, ref, campaignId, bonusEntry, {
        key: bonusKey,
        uid,
        amount: Number(test.get("completionBonus") ?? 0),
        testId,
      });
    }
    return {
      uid,
      app: String(test.get("appName") ?? ""),
      outcome,
      bonus: Number(test.get("completionBonus") ?? 0),
    };
  });
  if (result?.outcome === "failed") await notifySafely(result.uid, { ...messages.testFailed(result.app), refId: testId });
  if (result?.outcome === "completed") await notifySafely(result.uid, { ...messages.testCompleted(result.app, result.bonus), refId: testId });
}
