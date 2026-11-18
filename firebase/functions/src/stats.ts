import { DocumentReference, FieldValue, getFirestore, Transaction } from "firebase-admin/firestore";

export type StatField = "testsStarted" | "testsCompleted" | "testsFailed" | "testsLeft" | "daysPassed";

/** Tester counters on users/{uid}.stats (server-only fields; the client cannot write them). */
export function bumpStat(tx: Transaction, uid: string, field: StatField, by = 1): void {
  const ref: DocumentReference = getFirestore().collection("users").doc(uid);
  tx.set(ref, { stats: { [field]: FieldValue.increment(by) } }, { merge: true });
}

/** Share of finished tests that were completed, 0-100; null until a test has finished. */
export function reliability(stats: Partial<Record<StatField, number>>): number | null {
  const done = (stats.testsCompleted ?? 0) + (stats.testsFailed ?? 0) + (stats.testsLeft ?? 0);
  if (done === 0) return null;
  return Math.round(((stats.testsCompleted ?? 0) / done) * 100);
}
