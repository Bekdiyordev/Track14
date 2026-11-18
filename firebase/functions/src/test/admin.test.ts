import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { CONFIG_SCHEMAS, PushTarget, restoredStatus, validSendAt } from "../admin";

describe("admin: config validation", () => {
  it("app version needs latest >= min", () => {
    assert.equal(CONFIG_SCHEMAS.app_version.safeParse({ minBuild: 3, latestBuild: 5 }).success, true);
    assert.equal(CONFIG_SCHEMAS.app_version.safeParse({ minBuild: 5, latestBuild: 3 }).success, false);
    assert.equal(CONFIG_SCHEMAS.app_version.safeParse({ minBuild: 1, latestBuild: 1, hack: true }).success, false);
  });
  it("support links are restricted to known hosts", () => {
    assert.equal(CONFIG_SCHEMAS.support.safeParse({ telegram: "https://t.me/track14" }).success, true);
    assert.equal(CONFIG_SCHEMAS.support.safeParse({ telegram: "https://evil.io/t.me" }).success, false);
    assert.equal(CONFIG_SCHEMAS.support.safeParse({ groupUrl: "https://groups.google.com/g/track14-testers" }).success, true);
  });
  it("flags and economy", () => {
    assert.equal(CONFIG_SCHEMAS.flags.safeParse({ maintenance: true, newApps: true, purchases: false }).success, true);
    assert.equal(CONFIG_SCHEMAS.flags.safeParse({ maintenance: "yes", newApps: true, purchases: true }).success, false);
    assert.equal(CONFIG_SCHEMAS.economy.safeParse({ standardFee: 900 }).success, true);
    assert.equal(CONFIG_SCHEMAS.economy.safeParse({ standardFee: -1 }).success, false);
    assert.equal(CONFIG_SCHEMAS.economy.safeParse({ maxActiveTests: 0 }).success, false);
  });
});

describe("admin: push", () => {
  const now = Date.UTC(2026, 10, 18);
  it("targets", () => {
    assert.equal(PushTarget.safeParse({ kind: "all" }).success, true);
    assert.equal(PushTarget.safeParse({ kind: "users", uids: ["abc123"] }).success, true);
    assert.equal(PushTarget.safeParse({ kind: "users", uids: [] }).success, false);
    assert.equal(PushTarget.safeParse({ kind: "users", uids: ["../x"] }).success, false);
  });
  it("send time: now to 30 days ahead", () => {
    assert.equal(validSendAt(undefined, now), now);
    assert.equal(validSendAt(now + 3_600_000, now), now + 3_600_000);
    assert.throws(() => validSendAt(now - 3_600_000, now));
    assert.throws(() => validSendAt(now + 31 * 86_400_000, now));
  });
});

describe("admin: suspension", () => {
  it("lifting a suspension restores the campaign state", () => {
    assert.equal(restoredStatus("active"), "active");
    assert.equal(restoredStatus("published"), "published");
    assert.equal(restoredStatus(undefined), "draft");
  });
});
