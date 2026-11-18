import { FieldValue, getFirestore, Timestamp } from "firebase-admin/firestore";
import { onDocumentWritten } from "firebase-functions/v2/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { z } from "zod";
import { buildCatalogEntry, isValidPackageName } from "./catalog";
import { featurePrice, fee, loadEconomy, rewardPerDay } from "./economy";
import { docId, parse, requireActiveUser } from "./guards";
import { applyEntry, readEntry, walletRef } from "./ledger";
import { callableOptions, REGION } from "./options";
import { ledgerKey } from "./rules";

const db = () => getFirestore();

const CreateCampaign = z.object({ appId: docId, plan: z.enum(["standard", "pro"]) }).strict();

/**
 * Owner pays the listing fee once and the app goes live. One campaign per app:
 * the campaign id equals the app id, which also makes this call idempotent.
 */
export const createCampaign = onCall(callableOptions, async (req) => {
  const uid = await requireActiveUser(req);
  const { appId, plan } = parse(CreateCampaign, req.data);
  const flags = await db().doc("config/flags").get();
  if (flags.get("newApps") === false) throw new HttpsError("unavailable", "new_apps_disabled");
  const economy = await loadEconomy();
  const price = fee(economy, plan);
  const key = ledgerKey(appId, "listing");

  return db().runTransaction(async (tx) => {
    const appRef = db().collection("apps").doc(appId);
    const campaignRef = db().collection("campaigns").doc(appId);
    const [app, campaign, wallet, entry] = await Promise.all([
      tx.get(appRef),
      tx.get(campaignRef),
      tx.get(walletRef(uid)),
      readEntry(tx, key),
    ]);
    if (!app.exists || app.get("ownerUid") !== uid) throw new HttpsError("not-found", "app_not_found");
    if (campaign.exists) return { campaignId: appId, created: false };
    if (app.get("status") !== "draft") throw new HttpsError("failed-precondition", "app_not_draft");

    // One live listing per package: two owners cannot list the same app.
    const samePackage = await tx.get(
      db()
        .collection("apps")
        .where("packageName", "==", app.get("packageName"))
        .where("status", "in", ["active", "paused"])
        .limit(1),
    );
    if (!samePackage.empty) throw new HttpsError("already-exists", "package_in_use");

    const balance = Number(wallet.get("balance") ?? 0);
    if (balance < price) throw new HttpsError("failed-precondition", "insufficient_balance");

    applyEntry(tx, entry, { key, uid, amount: -price, type: "listing_fee", refId: appId });
    tx.set(campaignRef, {
      appId,
      ownerUid: uid,
      plan,
      feePaid: price,
      rewardPerDay: rewardPerDay(economy, plan),
      completionBonus: economy.completionBonus,
      testersNeeded: economy.testersNeeded,
      status: "active",
      startedAt: FieldValue.serverTimestamp(),
      featuredUntil: null,
      stats: { joined: 0, completed14: 0, failed: 0, left: 0 },
    });
    tx.update(appRef, { status: "active" });
    return { campaignId: appId, created: true };
  });
});

const SetStatus = z.object({ appId: docId, action: z.enum(["pause", "resume", "publish"]) }).strict();

/** Allowed owner transitions. Paused and published apps leave the catalog; running tests continue. */
export function nextStatus(current: string, action: "pause" | "resume" | "publish"): string | null {
  if (action === "pause") return current === "active" ? "paused" : null;
  if (action === "resume") return current === "paused" ? "active" : null;
  return current === "active" || current === "paused" ? "published" : null;
}

export const ownerSetStatus = onCall(callableOptions, async (req) => {
  const uid = await requireActiveUser(req);
  const { appId, action } = parse(SetStatus, req.data);
  return db().runTransaction(async (tx) => {
    const appRef = db().collection("apps").doc(appId);
    const campaignRef = db().collection("campaigns").doc(appId);
    const [app, campaign] = await Promise.all([tx.get(appRef), tx.get(campaignRef)]);
    if (!app.exists || app.get("ownerUid") !== uid || !campaign.exists) {
      throw new HttpsError("not-found", "app_not_found");
    }
    if (app.get("status") === "suspended") throw new HttpsError("failed-precondition", "suspended");
    const current = String(campaign.get("status"));
    const next = nextStatus(current, action);
    if (!next) throw new HttpsError("failed-precondition", "invalid_transition");
    const extra = next === "published" ? { publishedAt: FieldValue.serverTimestamp() } : {};
    tx.update(campaignRef, { status: next, ...extra });
    tx.update(appRef, { status: next });
    return { status: next };
  }).then(async (result) => {
    if (result.status === "published") await writePublished(appId);
    return result;
  });
});

/** Public "published through Track14" entry: shown in the app with a Play Store link. */
export function buildPublishedEntry(app: Record<string, unknown>, campaign: Record<string, unknown>, ratings: number[]) {
  const stats = (campaign.stats ?? {}) as Record<string, unknown>;
  const n = (v: unknown) => (typeof v === "number" && v > 0 ? Math.floor(v) : 0);
  const valid = ratings.filter((r) => Number.isInteger(r) && r >= 1 && r <= 5);
  return {
    name: String(app.name ?? "").slice(0, 60),
    category: typeof app.category === "string" ? app.category : "",
    packageName: isValidPackageName(app.packageName) ? app.packageName : "",
    testers: n(stats.joined),
    completed: n(stats.completed14),
    feedbackCount: valid.length,
    rating: valid.length ? Math.round((valid.reduce((a, b) => a + b, 0) / valid.length) * 10) / 10 : null,
    publishedAt: FieldValue.serverTimestamp(),
  };
}

async function writePublished(appId: string): Promise<void> {
  const [app, campaign, feedback] = await Promise.all([
    db().collection("apps").doc(appId).get(),
    db().collection("campaigns").doc(appId).get(),
    db().collection("feedback").where("appId", "==", appId).select("rating").get(),
  ]);
  if (!app.exists || !campaign.exists) return;
  const entry = buildPublishedEntry(app.data()!, campaign.data()!, feedback.docs.map((d) => Number(d.get("rating"))));
  const ref = db().collection("published").doc(appId);
  await db().runTransaction(async (tx) => {
    const existing = await tx.get(ref);
    tx.set(ref, entry);
    if (!existing.exists) {
      tx.set(
        db().doc("stats/published"),
        { count: FieldValue.increment(1), testers: FieldValue.increment(entry.testers) },
        { merge: true },
      );
    }
  });
}

const FeatureApp = z
  .object({
    appId: docId,
    days: z.union([z.literal(1), z.literal(3), z.literal(7)]),
    /** Client-generated id: a retried call cannot charge twice. */
    requestId: z.string().regex(/^[A-Za-z0-9_-]{8,64}$/),
  })
  .strict();

/** Extends the top placement from now (or from the current end, if still running). */
export function nextFeaturedUntil(currentMs: number | null, nowMs: number, days: number): number {
  return Math.max(currentMs ?? 0, nowMs) + days * 86_400_000;
}

export const featureApp = onCall(callableOptions, async (req) => {
  const uid = await requireActiveUser(req);
  const { appId, days, requestId } = parse(FeatureApp, req.data);
  const economy = await loadEconomy();
  const price = featurePrice(economy, days);
  const key = ledgerKey(appId, "feature", requestId);

  return db().runTransaction(async (tx) => {
    const campaignRef = db().collection("campaigns").doc(appId);
    const [campaign, app, wallet, entry] = await Promise.all([
      tx.get(campaignRef),
      tx.get(db().collection("apps").doc(appId)),
      tx.get(walletRef(uid)),
      readEntry(tx, key),
    ]);
    if (!campaign.exists || campaign.get("ownerUid") !== uid) throw new HttpsError("not-found", "app_not_found");
    if (entry.exists) return { featuredUntil: (campaign.get("featuredUntil") as Timestamp | null)?.toMillis() ?? null };
    if (campaign.get("status") !== "active" || app.get("status") === "suspended") {
      throw new HttpsError("failed-precondition", "campaign_not_active");
    }
    if (Number(wallet.get("balance") ?? 0) < price) throw new HttpsError("failed-precondition", "insufficient_balance");

    const current = (campaign.get("featuredUntil") as Timestamp | null)?.toMillis() ?? null;
    const until = nextFeaturedUntil(current, Date.now(), days);
    tx.update(campaignRef, { featuredUntil: Timestamp.fromMillis(until) });
    applyEntry(tx, entry, { key, uid, amount: -price, type: "feature", refId: appId });
    return { featuredUntil: until };
  });
});


/** Keeps `catalog/{appId}` equal to the public projection of app + campaign. */
async function syncCatalog(appId: string): Promise<void> {
  const [app, campaign] = await Promise.all([
    db().collection("apps").doc(appId).get(),
    db().collection("campaigns").doc(appId).get(),
  ]);
  const entry = buildCatalogEntry(app.data(), campaign.data());
  const ref = db().collection("catalog").doc(appId);
  if (entry) await ref.set(entry);
  else await ref.delete();
}

export const onCampaignWritten = onDocumentWritten(
  { document: "campaigns/{appId}", region: REGION },
  (event) => syncCatalog(event.params.appId),
);

export const onAppWritten = onDocumentWritten(
  { document: "apps/{appId}", region: REGION },
  (event) => syncCatalog(event.params.appId),
);
