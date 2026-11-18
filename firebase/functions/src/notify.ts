import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { getMessaging } from "firebase-admin/messaging";
import { logger } from "firebase-functions/v2";

export type NotificationType =
  | "test_failed"
  | "test_completed"
  | "tester_joined"
  | "feedback_received"
  | "daily_reminder"
  | "purchase_refunded"
  | "app_suspended"
  | "app_restored"
  | "admin_message";

export type Lang = "uz" | "ru" | "en";
export interface Text { title: string; body: string }

/** A notification in every app language; the recipient's `users/{uid}.lang` picks one. */
export interface Message {
  type: NotificationType;
  text: Record<Lang, Text>;
  refId?: string;
}

const t3 = (uz: Text, ru: Text, en: Text): Record<Lang, Text> => ({ uz, ru, en });

/** Admin-written text: the same words for every language. */
export function plain(type: NotificationType, title: string, body: string, refId?: string): Message {
  const text = { title, body };
  return { type, text: t3(text, text, text), refId };
}

export function langOf(value: unknown): Lang {
  return value === "ru" || value === "en" ? value : "uz";
}

/** Short texts: they are shown on the lock screen. */
export const messages = {
  testFailed: (app: string): Message => ({
    type: "test_failed",
    text: t3(
      { title: "Test uzildi", body: `${app}: kunlar o'tkazib yuborildi, test to'xtadi.` },
      { title: "Тест прерван", body: `${app}: пропущены дни, тест остановлен.` },
      { title: "Test ended", body: `${app}: days were missed, the test stopped.` },
    ),
  }),
  testCompleted: (app: string, bonus: number): Message => ({
    type: "test_completed",
    text: t3(
      { title: "14 kun yakunlandi", body: `${app}: +${bonus} Coin14 bonus yozildi.` },
      { title: "14 дней завершены", body: `${app}: начислен бонус +${bonus} Coin14.` },
      { title: "14 days completed", body: `${app}: +${bonus} Coin14 bonus added.` },
    ),
  }),
  testerJoined: (app: string, joined: number, needed: number): Message => ({
    type: "tester_joined",
    text: t3(
      { title: "Yangi tester", body: `${app}: ${joined} / ${needed} tester.` },
      { title: "Новый тестер", body: `${app}: ${joined} / ${needed} тестеров.` },
      { title: "New tester", body: `${app}: ${joined} / ${needed} testers.` },
    ),
  }),
  feedbackReceived: (app: string, rating: number): Message => {
    const stars = "★".repeat(Math.max(0, Math.min(5, rating)));
    return {
      type: "feedback_received",
      text: t3(
        { title: "Yangi fikr", body: `${app}: ${stars}` },
        { title: "Новый отзыв", body: `${app}: ${stars}` },
        { title: "New feedback", body: `${app}: ${stars}` },
      ),
    };
  },
  dailyReminder: (count: number): Message => ({
    type: "daily_reminder",
    text: t3(
      { title: "Bugungi testlar", body: `${count} ta ilova sizni kutmoqda. Har biriga 30 soniya yetadi.` },
      { title: "Тесты на сегодня", body: `Вас ждут приложения: ${count}. На каждое хватит 30 секунд.` },
      { title: "Today's tests", body: `${count} apps are waiting for you. 30 seconds each is enough.` },
    ),
  }),
  appSuspended: (app: string, reason: string): Message => ({
    type: "app_suspended",
    text: t3(
      { title: "Ilova bloklandi", body: `${app}: ${reason || "qoidalar buzilgan"}` },
      { title: "Приложение заблокировано", body: `${app}: ${reason || "нарушены правила"}` },
      { title: "App suspended", body: `${app}: ${reason || "rules were broken"}` },
    ),
  }),
  appRestored: (app: string): Message => ({
    type: "app_restored",
    text: t3(
      { title: "Ilova tiklandi", body: `${app} yana faol.` },
      { title: "Приложение восстановлено", body: `${app} снова активно.` },
      { title: "App restored", body: `${app} is active again.` },
    ),
  }),
};

const MAX_TOKENS = 10;

/** In-app inbox entry + push to every registered device; dead tokens are removed. */
export async function notify(uid: string, message: Message): Promise<void> {
  const db = getFirestore();
  const user = db.collection("users").doc(uid);
  const { title, body } = message.text[langOf((await user.get()).get("lang"))];
  await user.collection("notifications").add({
    type: message.type,
    title,
    body,
    refId: message.refId ?? null,
    read: false,
    createdAt: FieldValue.serverTimestamp(),
  });

  const tokens = await user.collection("tokens").limit(MAX_TOKENS).get();
  if (tokens.empty) return;
  const result = await getMessaging().sendEachForMulticast({
    tokens: tokens.docs.map((d) => d.id),
    notification: { title, body },
    data: { type: message.type, refId: message.refId ?? "" },
    android: { priority: "normal", notification: { channelId: channelFor(message.type) } },
  });
  const dead = result.responses
    .map((r, i) => ({ r, id: tokens.docs[i].id }))
    .filter(({ r }) => !r.success && isDeadToken(r.error?.code))
    .map(({ id }) => user.collection("tokens").doc(id).delete());
  await Promise.all(dead);
  if (result.failureCount > 0) logger.info("push partial failure", { uid, failures: result.failureCount });
}

export function channelFor(type: NotificationType): string {
  return type === "daily_reminder" ? "reminders" : "updates";
}

export function isDeadToken(code: string | undefined): boolean {
  return code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token";
}

/** Notifications must never break the money path: failures are logged, not thrown. */
export async function notifySafely(uid: string, message: Message): Promise<void> {
  try {
    await notify(uid, message);
  } catch (e) {
    logger.warn("notify failed", { uid, type: message.type, error: String(e) });
  }
}
