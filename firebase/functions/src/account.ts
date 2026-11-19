import { getAuth } from "firebase-admin/auth";
import { DocumentReference, FieldValue, getFirestore } from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { z } from "zod";
import { auditExpiry } from "./admin";
import { parse } from "./guards";
import { callableOptions } from "./options";

const db = () => getFirestore();

/** The app sends this literal so a stray call can never wipe an account. */
const DeleteAccount = z.object({ confirm: z.literal("DELETE") }).strict();

/** Firestore allows 500 writes per batch; stay below it. */
const BATCH = 400;

type Write = (batch: FirebaseFirestore.WriteBatch) => void;

/** Owner's campaigns after deletion: running ones are closed, finished history is kept. */
export function campaignStatusAfterOwnerDeleted(status: string): string | null {
  return status === "active" || status === "paused" || status === "draft" ? "removed" : null;
}

/** Tests the deleted user was running end as "left" without a penalty (there is no wallet any more). */
export function closesTest(status: string): boolean {
  return status === "active" || status === "joining";
}

/**
 * Play policy: an account created in the app can be deleted from the app.
 * Removes the profile, wallet, tokens and inbox, closes running tests and campaigns
 * (their apps leave the catalog through onAppWritten), anonymises feedback and finally
 * deletes the Auth user. Ledger and purchase records stay for refunds and accounting,
 * keyed by an id that no longer resolves to a person.
 * Blocked users may delete too: deletion is a right, not an action in the service.
 */
export const deleteAccount = onCall(callableOptions, async (req) => {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "sign_in_required");
  parse(DeleteAccount, req.data);

  const userRef = db().collection("users").doc(uid);
  const [tests, apps, campaigns, feedback, tokens, inbox] = await Promise.all([
    db().collection("tests").where("testerUid", "==", uid).get(),
    db().collection("apps").where("ownerUid", "==", uid).get(),
    db().collection("campaigns").where("ownerUid", "==", uid).get(),
    db().collection("feedback").where("testerUid", "==", uid).get(),
    userRef.collection("tokens").get(),
    userRef.collection("notifications").get(),
  ]);

  const writes: Write[] = [];
  for (const t of tests.docs) {
    if (!closesTest(String(t.get("status")))) continue;
    writes.push((b) => b.update(t.ref, { status: "left", endedReason: "account_deleted", endedAt: FieldValue.serverTimestamp() }));
    const campaignId = t.get("campaignId");
    if (typeof campaignId === "string" && campaignId) {
      const ref = db().collection("campaigns").doc(campaignId);
      writes.push((b) => b.update(ref, { "stats.left": FieldValue.increment(1) }));
    }
  }
  for (const c of campaigns.docs) {
    const next = campaignStatusAfterOwnerDeleted(String(c.get("status")));
    if (next) writes.push((b) => b.update(c.ref, { status: next, endedAt: FieldValue.serverTimestamp() }));
  }
  for (const a of apps.docs) {
    if (a.get("status") !== "published") writes.push((b) => b.update(a.ref, { status: "removed", ownerDeleted: true }));
    else writes.push((b) => b.update(a.ref, { ownerDeleted: true }));
  }
  for (const f of feedback.docs) writes.push((b) => b.update(f.ref, { testerUid: "deleted" }));
  const deletes: DocumentReference[] = [...tokens.docs, ...inbox.docs].map((d) => d.ref);
  deletes.push(userRef, db().collection("wallets").doc(uid));
  for (const ref of deletes) writes.push((b) => b.delete(ref));
  const audit = db().collection("auditLogs").doc();
  writes.push((b) =>
    b.set(audit, {
      actor: uid,
      action: "delete_account",
      target: uid,
      details: { tests: tests.size, apps: apps.size },
      createdAt: FieldValue.serverTimestamp(),
      expireAt: auditExpiry(),
    }),
  );

  for (let i = 0; i < writes.length; i += BATCH) {
    const batch = db().batch();
    writes.slice(i, i + BATCH).forEach((w) => w(batch));
    await batch.commit();
  }
  await getAuth().deleteUser(uid);
  return { ok: true };
});
