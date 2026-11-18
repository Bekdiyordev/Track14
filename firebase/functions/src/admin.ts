import { randomUUID } from "node:crypto";
import { getAuth } from "firebase-admin/auth";
import { FieldValue, getFirestore, Transaction } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions/v2";
import { CallableRequest, HttpsError, onCall } from "firebase-functions/v2/https";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { z } from "zod";
import { docId, parse } from "./guards";
import { applyEntry, readEntry } from "./ledger";
import { messages, notifySafely, plain } from "./notify";
import { callableOptions, REGION } from "./options";
import { ledgerKey } from "./rules";

export type Role = "superadmin" | "moderator" | "support";
const db = () => getFirestore();
const uidSchema = z.string().regex(/^[A-Za-z0-9]{1,128}$/);

/** Role comes from a custom claim set only by adminSetRole or the setRole script. */
export async function requireRole(req: CallableRequest<unknown>, allowed: readonly Role[]): Promise<{ uid: string; role: Role }> {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "sign_in_required");
  const role = req.auth?.token?.role;
  if (!allowed.includes(role as Role)) throw new HttpsError("permission-denied", "staff_only");
  return { uid, role: role as Role };
}

/** Every admin action leaves an audit record (read-only for staff in the rules). */
function audit(tx: Transaction | null, actor: string, action: string, target: string, details: Record<string, unknown>) {
  const ref = db().collection("auditLogs").doc();
  const entry = { actor, action, target, details, createdAt: FieldValue.serverTimestamp() };
  if (tx) tx.set(ref, entry);
  else return ref.set(entry);
  return undefined;
}

// ------------------------------------------------------------------ roles

/** role omitted or null removes the staff role (the Android SDK drops null map values). */
const SetRole = z.object({ uid: uidSchema, role: z.enum(["superadmin", "moderator", "support"]).nullable().default(null) }).strict();

export const adminSetRole = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin"]);
  const { uid, role } = parse(SetRole, req.data);
  if (uid === actor.uid && role !== "superadmin") throw new HttpsError("failed-precondition", "cannot_demote_self");
  const user = await getAuth().getUser(uid);
  await getAuth().setCustomUserClaims(uid, { ...(user.customClaims ?? {}), role: role ?? null });
  await audit(null, actor.uid, "set_role", uid, { role });
  return { ok: true };
});

// ------------------------------------------------------------------ users

const BlockUser = z.object({ uid: uidSchema, blocked: z.boolean(), reason: z.string().max(300).default("") }).strict();

/**
 * Blocking: the user can no longer act (guards), their sessions are revoked, their apps leave
 * the catalog and the rollover skips their tests. Coins stay frozen, not removed.
 */
export const adminBlockUser = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin", "moderator"]);
  const { uid, blocked, reason } = parse(BlockUser, req.data);
  if (uid === actor.uid) throw new HttpsError("failed-precondition", "cannot_block_self");
  const apps = await db().collection("apps").where("ownerUid", "==", uid).get();
  await db().runTransaction(async (tx) => {
    tx.set(db().collection("users").doc(uid), { blocked, blockedReason: blocked ? reason : null }, { merge: true });
    apps.docs.forEach((a) => tx.update(a.ref, { ownerBlocked: blocked }));
    audit(tx, actor.uid, blocked ? "block_user" : "unblock_user", uid, { reason });
  });
  if (blocked) await getAuth().revokeRefreshTokens(uid);
  return { ok: true };
});

const AdjustCoins = z
  .object({
    uid: uidSchema,
    amount: z.number().int().min(-100_000).max(100_000).refine((n) => n !== 0),
    reason: z.string().min(3).max(300),
  })
  .strict();

/** Manual correction (support cases). Superadmin only; always audited. */
export const adminAdjustCoins = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin"]);
  const { uid, amount, reason } = parse(AdjustCoins, req.data);
  const key = ledgerKey("adjust", randomUUID().replace(/-/g, ""));
  await db().runTransaction(async (tx) => {
    const entry = await readEntry(tx, key);
    applyEntry(tx, entry, { key, uid, amount, type: "adjustment", refId: actor.uid });
    audit(tx, actor.uid, "adjust_coins", uid, { amount, reason });
  });
  return { ok: true };
});

// ------------------------------------------------------------------ apps

const SuspendApp = z
  .object({ appId: docId, suspend: z.boolean(), reason: z.string().max(300).default(""), refundFee: z.boolean().default(false) })
  .strict();

/** Status an app returns to when a suspension is lifted. */
export function restoredStatus(campaignStatus: unknown): string {
  return campaignStatus === "active" || campaignStatus === "paused" || campaignStatus === "published" ? String(campaignStatus) : "draft";
}

/**
 * Suspend hides the app (catalog) and stops owner actions. Refunding the listing fee is the
 * admin's decision per case (plan decision), paid once with an idempotent key.
 */
export const adminSuspendApp = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin", "moderator"]);
  const { appId, suspend, reason, refundFee } = parse(SuspendApp, req.data);
  const refundKey = ledgerKey(appId, "listing_refund");

  const owner = await db().runTransaction(async (tx) => {
    const appRef = db().collection("apps").doc(appId);
    const [app, campaign, entry] = await Promise.all([
      tx.get(appRef),
      tx.get(db().collection("campaigns").doc(appId)),
      readEntry(tx, refundKey),
    ]);
    if (!app.exists) throw new HttpsError("not-found", "app_not_found");
    const ownerUid = String(app.get("ownerUid"));
    tx.update(appRef, {
      status: suspend ? "suspended" : restoredStatus(campaign.get("status")),
      suspendedReason: suspend ? reason : null,
    });
    if (suspend && refundFee && campaign.exists) {
      const fee = Number(campaign.get("feePaid") ?? 0);
      if (fee > 0) applyEntry(tx, entry, { key: refundKey, uid: ownerUid, amount: fee, type: "refund", refId: appId });
    }
    audit(tx, actor.uid, suspend ? "suspend_app" : "unsuspend_app", appId, { reason, refundFee });
    return { uid: ownerUid, name: String(app.get("name") ?? "") };
  });

  const message = suspend ? messages.appSuspended(owner.name, reason) : messages.appRestored(owner.name);
  await notifySafely(owner.uid, { ...message, refId: appId });
  return { ok: true };
});

// ------------------------------------------------------------------ reports

const ResolveReport = z
  .object({ reportId: docId, status: z.enum(["resolved", "dismissed"]), note: z.string().max(500).default("") })
  .strict();

export const adminResolveReport = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin", "moderator", "support"]);
  const { reportId, status, note } = parse(ResolveReport, req.data);
  await db().runTransaction(async (tx) => {
    const ref = db().collection("reports").doc(reportId);
    const report = await tx.get(ref);
    if (!report.exists) throw new HttpsError("not-found", "report_not_found");
    tx.update(ref, { status, resolvedBy: actor.uid, note, resolvedAt: FieldValue.serverTimestamp() });
    audit(tx, actor.uid, "resolve_report", reportId, { status });
  });
  return { ok: true };
});

// ------------------------------------------------------------------ config

const https = z.string().url().startsWith("https://").max(300);

export const CONFIG_SCHEMAS = {
  app_version: z
    .object({
      minBuild: z.number().int().min(1),
      latestBuild: z.number().int().min(1),
      updateUrl: https.optional(),
      message: z.string().max(300).optional(),
    })
    .strict()
    .refine((v) => v.latestBuild >= v.minBuild, "latestBuild < minBuild"),
  support: z
    .object({
      telegram: z.string().startsWith("https://t.me/").max(200).optional(),
      email: z.string().email().max(120).optional(),
      groupUrl: z.string().startsWith("https://groups.google.com/").max(200).optional(),
      faqUrl: https.optional(),
    })
    .strict(),
  flags: z
    .object({
      maintenance: z.boolean(),
      maintenanceMessage: z.string().max(300).optional(),
      newApps: z.boolean(),
      purchases: z.boolean(),
    })
    .strict(),
  economy: z
    .object({
      standardFee: z.number().int().min(0),
      proFee: z.number().int().min(0),
      standardRewardPerDay: z.number().int().min(0),
      proRewardPerDay: z.number().int().min(0),
      completionBonus: z.number().int().min(0),
      feedbackReward: z.number().int().min(0),
      failedPenalty: z.number().int().min(0),
      leftPenalty: z.number().int().min(0),
      welcomeBonus: z.number().int().min(0),
      maxActiveTests: z.number().int().min(1).max(100),
      testersNeeded: z.number().int().min(1).max(100),
      feature1Price: z.number().int().min(0),
      feature3Price: z.number().int().min(0),
      feature7Price: z.number().int().min(0),
    })
    .partial()
    .strict(),
} as const;

const SetConfig = z.object({ doc: z.enum(["app_version", "support", "flags", "economy"]), value: z.record(z.string(), z.unknown()) }).strict();

/** Validated writes to the public config documents the app reads (version, support links, flags, economy). */
export const adminSetConfig = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin"]);
  const { doc, value } = parse(SetConfig, req.data);
  const valid = parse(CONFIG_SCHEMAS[doc], value);
  await db().runTransaction(async (tx) => {
    tx.set(db().collection("config").doc(doc), { ...valid, updatedAt: FieldValue.serverTimestamp() }, { merge: doc === "economy" });
    audit(tx, actor.uid, "set_config", doc, valid as Record<string, unknown>);
  });
  return { ok: true };
});

// ------------------------------------------------------------------ push

export const BROADCAST_TOPIC = "all";

export const PushTarget = z.discriminatedUnion("kind", [
  z.object({ kind: z.literal("all") }).strict(),
  z.object({ kind: z.literal("users"), uids: z.array(uidSchema).min(1).max(500) }).strict(),
]);

const SendPush = z
  .object({
    target: PushTarget,
    title: z.string().min(1).max(65),
    body: z.string().min(1).max(240),
    /** Epoch millis; omitted = send with the next dispatch (within a minute). At most 30 days ahead. */
    sendAt: z.number().int().optional(),
  })
  .strict();

export function validSendAt(sendAt: number | undefined, now: number): number {
  const at = sendAt ?? now;
  if (at < now - 60_000 || at > now + 30 * 86_400_000) throw new HttpsError("invalid-argument", "invalid_send_at");
  return at;
}

export const adminSendPush = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin", "support"]);
  const { target, title, body, sendAt } = parse(SendPush, req.data);
  const at = validSendAt(sendAt, Date.now());
  const ref = db().collection("pushJobs").doc();
  await db().runTransaction(async (tx) => {
    tx.set(ref, { target, title, body, sendAt: new Date(at), status: "scheduled", createdBy: actor.uid, createdAt: FieldValue.serverTimestamp() });
    audit(tx, actor.uid, "schedule_push", ref.id, { kind: target.kind, sendAt: at });
  });
  return { jobId: ref.id };
});

const CancelPush = z.object({ jobId: docId }).strict();

export const adminCancelPush = onCall(callableOptions, async (req) => {
  const actor = await requireRole(req, ["superadmin", "support"]);
  const { jobId } = parse(CancelPush, req.data);
  await db().runTransaction(async (tx) => {
    const ref = db().collection("pushJobs").doc(jobId);
    const job = await tx.get(ref);
    if (job.get("status") !== "scheduled") throw new HttpsError("failed-precondition", "not_scheduled");
    tx.update(ref, { status: "cancelled" });
    audit(tx, actor.uid, "cancel_push", jobId, {});
  });
  return { ok: true };
});

/** Every minute: send due jobs. A job is claimed in a transaction so it is sent once. */
export const dispatchPushes = onSchedule({ schedule: "every 1 minutes", region: REGION, timeoutSeconds: 120 }, async () => {
  const due = await db().collection("pushJobs").where("status", "==", "scheduled").where("sendAt", "<=", new Date()).limit(20).get();
  for (const doc of due.docs) {
    const claimed = await db().runTransaction(async (tx) => {
      const fresh = await tx.get(doc.ref);
      if (fresh.get("status") !== "scheduled") return false;
      tx.update(doc.ref, { status: "sending" });
      return true;
    });
    if (!claimed) continue;
    const { target, title, body } = doc.data() as { target: z.infer<typeof PushTarget>; title: string; body: string };
    try {
      let sent = 0;
      if (target.kind === "all") {
        await getMessaging().send({ topic: BROADCAST_TOPIC, notification: { title, body }, android: { notification: { channelId: "updates" } } });
      } else {
        for (const uid of target.uids) {
          await notifySafely(uid, plain("admin_message", title, body, doc.id));
          sent++;
        }
      }
      await doc.ref.update({ status: "sent", sentAt: FieldValue.serverTimestamp(), recipients: target.kind === "all" ? "all" : sent });
    } catch (e) {
      logger.error("push job failed", { jobId: doc.id, error: String(e) });
      await doc.ref.update({ status: "failed", error: String(e).slice(0, 300) });
    }
  }
});
