import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { buildCatalogEntry, isValidOptInUrl, isValidPackageName } from "../catalog";
import { DEFAULT_ECONOMY, fee, mergeEconomy, rewardPerDay } from "../economy";

const app = { name: " Lug'at Pro ", category: "Ta'lim", shortDesc: "Lug'at", ownerUid: "secret-owner", status: "active" };
const campaign = { status: "active", plan: "pro", rewardPerDay: 5, stats: { joined: 9 }, startedAt: 1, ownerUid: "secret-owner" };

describe("catalog projection", () => {
  it("lists an active campaign without owner data", () => {
    const entry = buildCatalogEntry(app, campaign)!;
    assert.equal(entry.name, "Lug'at Pro");
    assert.equal(entry.plan, "pro");
    assert.equal(entry.joined, 9);
    assert.equal(entry.needed, 12);
    assert.equal(entry.status, "active");
    assert.equal(JSON.stringify(entry).includes("secret-owner"), false);
  });
  it("hides paused, suspended, nameless or missing apps", () => {
    assert.equal(buildCatalogEntry(app, { ...campaign, status: "paused" }), null);
    assert.equal(buildCatalogEntry({ ...app, status: "suspended" }, campaign), null);
    assert.equal(buildCatalogEntry({ ...app, name: "  " }, campaign), null);
    assert.equal(buildCatalogEntry(undefined, campaign), null);
    assert.equal(buildCatalogEntry({ ...app, ownerBlocked: true }, campaign), null);
    assert.equal(buildCatalogEntry(app, undefined), null);
  });
  it("sanitises numbers", () => {
    const entry = buildCatalogEntry(app, { ...campaign, rewardPerDay: -3, stats: { joined: "9" } })!;
    assert.equal(entry.rewardPerDay, 0);
    assert.equal(entry.joined, 0);
  });
});

describe("economy config", () => {
  it("defaults match the plan", () => {
    assert.equal(fee(DEFAULT_ECONOMY, "standard"), 1000);
    assert.equal(fee(DEFAULT_ECONOMY, "pro"), 1800);
    assert.equal(rewardPerDay(DEFAULT_ECONOMY, "pro"), 5);
    assert.ok(DEFAULT_ECONOMY.welcomeBonus >= 3 * DEFAULT_ECONOMY.standardFee);
  });
  it("stored values override only valid integers", () => {
    const e = mergeEconomy({ standardFee: 900, proFee: -1, leftPenalty: "x", unknown: 5 });
    assert.equal(e.standardFee, 900);
    assert.equal(e.proFee, 1800);
    assert.equal(e.leftPenalty, 15);
    assert.equal("unknown" in e, false);
  });
});

describe("owner links", () => {
  it("keeps only valid package names and play testing links", () => {
    const entry = buildCatalogEntry(
      { ...app, packageName: "uz.app.lugat", optInUrl: "https://play.google.com/apps/testing/uz.app.lugat" },
      campaign,
    )!;
    assert.equal(entry.packageName, "uz.app.lugat");
    assert.equal(entry.optInUrl, "https://play.google.com/apps/testing/uz.app.lugat");
    const bad = buildCatalogEntry({ ...app, packageName: "x;y", optInUrl: "https://evil.io" }, campaign)!;
    assert.equal(bad.packageName, "");
    assert.equal(bad.optInUrl, "");
  });
  it("matches the Android checks", () => {
    assert.equal(isValidPackageName("track14"), false);
    assert.equal(isValidOptInUrl("https://play.google.com.evil.io/apps/testing/uz.a.b"), false);
    assert.equal(isValidOptInUrl("https://play.google.com/apps/testing/uz.a.b/"), true);
  });
});
