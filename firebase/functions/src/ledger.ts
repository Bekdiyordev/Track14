import { DocumentSnapshot, FieldValue, getFirestore, Transaction } from "firebase-admin/firestore";

export type TxType =
  | "welcome"
  | "purchase"
  | "earn_day"
  | "bonus"
  | "feedback"
  | "listing_fee"
  | "penalty"
  | "feature"
  | "refund"
  | "adjustment";

const db = () => getFirestore();
export const walletRef = (uid: string) => db().collection("wallets").doc(uid);
export const ledgerRef = (key: string) => db().collection("transactions").doc(key);

/** Read inside the transaction before any write (Firestore rule: reads first). */
export async function readEntry(tx: Transaction, key: string): Promise<DocumentSnapshot> {
  return tx.get(ledgerRef(key));
}

/**
 * Applies a coin movement exactly once: the ledger document id is the idempotency key,
 * so retries and duplicate triggers cannot pay twice. Returns false if it was already applied.
 */
export function applyEntry(
  tx: Transaction,
  existing: DocumentSnapshot,
  entry: { key: string; uid: string; amount: number; type: TxType; refId: string },
): boolean {
  if (existing.exists) return false;
  if (!Number.isInteger(entry.amount)) throw new Error("amount must be an integer");
  tx.set(
    walletRef(entry.uid),
    { balance: FieldValue.increment(entry.amount), updatedAt: FieldValue.serverTimestamp() },
    { merge: true },
  );
  tx.set(ledgerRef(entry.key), {
    uid: entry.uid,
    type: entry.type,
    amount: entry.amount,
    refId: entry.refId,
    createdAt: FieldValue.serverTimestamp(),
  });
  return true;
}
