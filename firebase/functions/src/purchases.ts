import { createHash } from "node:crypto";
import { androidpublisher, androidpublisher_v3, auth } from "@googleapis/androidpublisher";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { logger } from "firebase-functions/v2";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { onMessagePublished } from "firebase-functions/v2/pubsub";
import { z } from "zod";
import { parse, requireActiveUser } from "./guards";
import { applyEntry, readEntry } from "./ledger";
import { callableOptions, REGION } from "./options";
import { ledgerKey } from "./rules";

export const PACKAGE_NAME = "uz.beko404.track14";

/** Product id -> Coin14. Same list as CoinPacks on Android; prices live in Play Console. */
export const COIN_PACKS: Readonly<Record<string, number>> = {
  coins_500: 500,
  coins_1000: 1000,
  coins_2000: 2000,
  coins_5000: 5000,
  coins_10000: 10000,
};

const sha256 = (s: string) => createHash("sha256").update(s).digest("hex");

/** The app passes this as obfuscatedAccountId, so a purchase is bound to one Track14 user. */
export const accountIdOf = (uid: string) => sha256(uid);

/** Firestore id for a purchase token (tokens are long and opaque). */
export const tokenId = (token: string) => sha256(token).slice(0, 40);

export type PurchaseCheck = "ok" | "pending" | "not_purchased" | "wrong_account" | "wrong_product";

export function checkPurchase(
  p: Pick<androidpublisher_v3.Schema$ProductPurchase, "purchaseState" | "obfuscatedExternalAccountId" | "productId">,
  uid: string,
  productId: string,
): PurchaseCheck {
  if (p.productId && p.productId !== productId) return "wrong_product";
  if (p.purchaseState === 2) return "pending";
  if (p.purchaseState !== 0) return "not_purchased";
  if (p.obfuscatedExternalAccountId !== accountIdOf(uid)) return "wrong_account";
  return "ok";
}

let client: androidpublisher_v3.Androidpublisher | null = null;
function play(): androidpublisher_v3.Androidpublisher {
  // Uses the Functions service account; it must be invited in Play Console (see docs/SETUP.md).
  client ??= androidpublisher({
    version: "v3",
    auth: new auth.GoogleAuth({ scopes: ["https://www.googleapis.com/auth/androidpublisher"] }),
  });
  return client;
}

const Verify = z
  .object({
    productId: z.string().refine((id) => id in COIN_PACKS),
    purchaseToken: z.string().min(10).max(4096),
  })
  .strict();

/**
 * Credits a Coin14 pack exactly once after Google confirms the purchase, then consumes it
 * so the pack can be bought again. Safe to call again (app restarts, retries).
 */
export const verifyPurchase = onCall({ ...callableOptions, timeoutSeconds: 30 }, async (req) => {
  const uid = await requireActiveUser(req);
  const { productId, purchaseToken } = parse(Verify, req.data);

  let purchase: androidpublisher_v3.Schema$ProductPurchase;
  try {
    purchase = (await play().purchases.products.get({ packageName: PACKAGE_NAME, productId, token: purchaseToken })).data;
  } catch (e) {
    logger.warn("purchase lookup failed", { productId, error: String(e) });
    throw new HttpsError("invalid-argument", "invalid_purchase");
  }

  const check = checkPurchase(purchase, uid, productId);
  if (check === "pending") return { status: "pending" };
  if (check !== "ok") throw new HttpsError("failed-precondition", check);

  const coins = COIN_PACKS[productId];
  const id = tokenId(purchaseToken);
  const key = ledgerKey("purchase", id);
  const db = getFirestore();

  await db.runTransaction(async (tx) => {
    const ref = db.collection("purchases").doc(id);
    const [doc, entry] = await Promise.all([tx.get(ref), readEntry(tx, key)]);
    if (doc.exists) {
      if (doc.get("uid") !== uid) throw new HttpsError("permission-denied", "wrong_account");
      return;
    }
    tx.set(ref, {
      uid,
      productId,
      coins,
      orderId: purchase.orderId ?? null,
      state: "credited",
      createdAt: FieldValue.serverTimestamp(),
    });
    applyEntry(tx, entry, { key, uid, amount: coins, type: "purchase", refId: purchase.orderId ?? id });
  });

  if (purchase.consumptionState !== 1) {
    try {
      await play().purchases.products.consume({ packageName: PACKAGE_NAME, productId, token: purchaseToken });
    } catch (e) {
      // Coins are already credited; the client retries and this call is idempotent.
      logger.warn("consume failed", { productId, error: String(e) });
    }
  }
  return { status: "credited", coins };
});

interface VoidedNotification {
  packageName?: string;
  voidedPurchaseNotification?: { purchaseToken?: string; productType?: number };
}

/** Token of a voided one-time purchase in an RTDN message, or null if the message is not one. */
export function voidedTokenOf(message: VoidedNotification): string | null {
  if (message.packageName !== PACKAGE_NAME) return null;
  const v = message.voidedPurchaseNotification;
  if (!v?.purchaseToken || v.productType !== 2) return null;
  return v.purchaseToken;
}

/**
 * Real-time developer notifications (Pub/Sub topic `play-rtdn`). A refunded or charged-back
 * pack takes its Coin14 back; the balance may go negative, which blocks spending.
 */
export const onPlayRtdn = onMessagePublished({ topic: "play-rtdn", region: REGION }, async (event) => {
  const token = voidedTokenOf((event.data.message.json ?? {}) as VoidedNotification);
  if (!token) return;
  const id = tokenId(token);
  const key = ledgerKey("refund", id);
  const db = getFirestore();
  await db.runTransaction(async (tx) => {
    const ref = db.collection("purchases").doc(id);
    const [doc, entry] = await Promise.all([tx.get(ref), readEntry(tx, key)]);
    if (!doc.exists || doc.get("state") !== "credited") return;
    tx.update(ref, { state: "refunded", refundedAt: FieldValue.serverTimestamp() });
    applyEntry(tx, entry, {
      key,
      uid: doc.get("uid"),
      amount: -Number(doc.get("coins") ?? 0),
      type: "refund",
      refId: String(doc.get("orderId") ?? id),
    });
  });
});
