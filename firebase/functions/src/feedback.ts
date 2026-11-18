import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { onDocumentCreated } from "firebase-functions/v2/firestore";
import { loadEconomy } from "./economy";
import { applyEntry, readEntry } from "./ledger";
import { messages, notifySafely } from "./notify";
import { REGION } from "./options";
import { ledgerKey } from "./rules";

/**
 * First feedback per test pays the feedback reward (+2) once. The rules already checked that
 * the author is the tester of that test and that appId / ownerUid match it.
 */
export const onFeedbackCreated = onDocumentCreated({ document: "feedback/{id}", region: REGION }, async (event) => {
  const data = event.data?.data();
  if (!data) return;
  const testId = String(data.testId ?? "");
  const uid = String(data.testerUid ?? "");
  if (!testId || !uid) return;
  const economy = await loadEconomy();
  const db = getFirestore();
  const key = ledgerKey(testId, "feedback");

  const app = await db.runTransaction(async (tx) => {
    const ref = db.collection("tests").doc(testId);
    const [test, entry] = await Promise.all([tx.get(ref), readEntry(tx, key)]);
    if (!test.exists || test.get("testerUid") !== uid) return null;
    if (test.get("feedbackRewarded") !== true) {
      tx.update(ref, { feedbackRewarded: true });
      applyEntry(tx, entry, { key, uid, amount: economy.feedbackReward, type: "feedback", refId: event.params.id });
    }
    return { owner: String(test.get("ownerUid")), name: String(test.get("appName") ?? "") };
  });

  if (app) await notifySafely(app.owner, { ...messages.feedbackReceived(app.name, Number(data.rating ?? 0)), refId: String(data.appId ?? "") });
  await event.data?.ref.update({ processedAt: FieldValue.serverTimestamp() });
});
