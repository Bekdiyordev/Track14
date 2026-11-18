import { getFirestore } from "firebase-admin/firestore";
import { CallableRequest, HttpsError } from "firebase-functions/v2/https";
import { z } from "zod";

const STAFF = new Set(["superadmin", "moderator", "support"]);

export function isStaff(role: unknown): boolean {
  return typeof role === "string" && STAFF.has(role);
}

let flagsCache: { maintenance: boolean; at: number } | null = null;
const FLAGS_TTL_MS = 30_000;

/** `config/flags.maintenance`: while on, every user action is refused (staff excepted). */
async function maintenanceOn(): Promise<boolean> {
  if (flagsCache && Date.now() - flagsCache.at < FLAGS_TTL_MS) return flagsCache.maintenance;
  const snap = await getFirestore().doc("config/flags").get();
  flagsCache = { maintenance: snap.get("maintenance") === true, at: Date.now() };
  return flagsCache.maintenance;
}

/** Signed-in, not blocked, and the service is not in maintenance. Returns the uid. */
export async function requireActiveUser(req: CallableRequest<unknown>): Promise<string> {
  const uid = req.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "sign_in_required");
  const staff = isStaff(req.auth?.token?.role);
  if (!staff && (await maintenanceOn())) throw new HttpsError("unavailable", "maintenance");
  const user = await getFirestore().collection("users").doc(uid).get();
  if (user.get("blocked") === true) throw new HttpsError("permission-denied", "blocked");
  return uid;
}

/** Strict input parsing: unknown fields and wrong types are rejected. */
export function parse<T extends z.ZodType>(schema: T, data: unknown): z.infer<T> {
  const result = schema.safeParse(data);
  if (!result.success) throw new HttpsError("invalid-argument", "invalid_input");
  return result.data;
}

export const docId = z.string().regex(/^[A-Za-z0-9_-]{1,64}$/);
