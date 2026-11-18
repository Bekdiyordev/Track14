import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions/v2";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { messages, notifySafely } from "./notify";
import { REGION } from "./options";
import { dayNumber, TEST_DAYS } from "./rules";

export const REMINDER_HOUR = 19;

/** Local hour (0-23) of an instant in a time zone. */
export function localHour(epochMs: number, timeZone: string): number {
  const h = new Intl.DateTimeFormat("en-GB", { timeZone, hour: "2-digit", hourCycle: "h23" }).format(new Date(epochMs));
  return Number(h);
}

/** Hourly: at 19:00 in each tester's zone, remind about tests whose day is still open. */
export const dailyReminder = onSchedule({ schedule: "every 60 minutes", region: REGION, timeoutSeconds: 300 }, async () => {
  const db = getFirestore();
  const now = Date.now();
  const active = await db.collection("tests").where("status", "==", "active").select("testerUid", "timezone", "startedAt").get();
  const open = new Map<string, number>();

  await Promise.all(
    active.docs.map(async (doc) => {
      const tz = String(doc.get("timezone") ?? "UTC");
      const startedAt = doc.get("startedAt") as Timestamp | undefined;
      if (!startedAt || localHour(now, tz) !== REMINDER_HOUR) return;
      const day = dayNumber(startedAt.toMillis(), now, tz);
      if (day < 1 || day > TEST_DAYS) return;
      const today = await doc.ref.collection("days").doc(String(day)).get();
      if (today.get("status") === "passed") return;
      const uid = String(doc.get("testerUid"));
      open.set(uid, (open.get(uid) ?? 0) + 1);
    }),
  );

  await Promise.all([...open].map(([uid, count]) => notifySafely(uid, messages.dailyReminder(count))));
  logger.info("reminders sent", { users: open.size });
});
