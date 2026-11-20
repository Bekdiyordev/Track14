import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { channelFor, isDeadToken, langOf, messages, plain, pushAllowed } from "../notify";
import { localHour } from "../reminders";

describe("notifications", () => {
  it("push switches mute only reminders and owner news", () => {
    assert.equal(pushAllowed("daily_reminder", undefined, undefined), true);
    assert.equal(pushAllowed("daily_reminder", false, true), false);
    assert.equal(pushAllowed("tester_joined", true, false), false);
    assert.equal(pushAllowed("feedback_received", true, false), false);
    assert.equal(pushAllowed("test_failed", false, false), true);
    assert.equal(pushAllowed("app_suspended", false, false), true);
  });

  it("messages are short and typed", () => {
    const m = messages.dailyReminder(3);
    assert.equal(m.type, "daily_reminder");
    assert.ok(m.text.uz.body.startsWith("3 ta"));
    assert.ok(m.text.ru.body.includes("3"));
    assert.ok(m.text.en.body.startsWith("3 apps"));
    assert.ok(messages.testCompleted("Lug'at", 10).text.en.body.includes("+10"));
    assert.equal(messages.feedbackReceived("X", 4).text.uz.body, "X: ★★★★");
    assert.equal(messages.feedbackReceived("X", 9).text.uz.body, "X: ★★★★★");
  });
  it("language falls back to Uzbek", () => {
    assert.equal(langOf("ru"), "ru");
    assert.equal(langOf("de"), "uz");
    assert.equal(langOf(undefined), "uz");
    const p = plain("admin_message", "Salom", "Matn");
    assert.equal(p.text.en.title, "Salom");
  });
  it("channels and dead tokens", () => {
    assert.equal(channelFor("daily_reminder"), "reminders");
    assert.equal(channelFor("test_failed"), "updates");
    assert.equal(isDeadToken("messaging/registration-token-not-registered"), true);
    assert.equal(isDeadToken("messaging/internal-error"), false);
  });
  it("local hour follows the zone", () => {
    const t = Date.UTC(2026, 10, 17, 14, 30); // 19:30 in Samarkand
    assert.equal(localHour(t, "Asia/Samarkand"), 19);
    assert.equal(localHour(t, "UTC"), 14);
  });
});
