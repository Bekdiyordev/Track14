import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { buildPublishedEntry, nextFeaturedUntil } from "../campaigns";
import { DEFAULT_ECONOMY, featurePrice } from "../economy";
import { reliability } from "../stats";

describe("reliability", () => {
  it("is null until a test finished, then completed share", () => {
    assert.equal(reliability({ testsStarted: 3 }), null);
    assert.equal(reliability({ testsCompleted: 3, testsFailed: 1 }), 75);
    assert.equal(reliability({ testsCompleted: 1, testsLeft: 2 }), 33);
  });
});

describe("top placement", () => {
  const day = 86_400_000;
  it("prices follow the plan", () => {
    assert.equal(featurePrice(DEFAULT_ECONOMY, 1), 150);
    assert.equal(featurePrice(DEFAULT_ECONOMY, 3), 350);
    assert.equal(featurePrice(DEFAULT_ECONOMY, 7), 700);
  });
  it("extends from now or from the running end", () => {
    assert.equal(nextFeaturedUntil(null, 1_000, 1), 1_000 + day);
    assert.equal(nextFeaturedUntil(500, 1_000, 3), 1_000 + 3 * day);
    assert.equal(nextFeaturedUntil(5 * day, day, 1), 6 * day);
  });
});

describe("published entry", () => {
  it("averages valid ratings and drops bad values", () => {
    const e = buildPublishedEntry(
      { name: "Lug'at", category: "Ta'lim", packageName: "uz.app.lugat" },
      { stats: { joined: 15, completed14: 12 } },
      [5, 4, 9, 0, 4],
    );
    assert.equal(e.rating, 4.3);
    assert.equal(e.feedbackCount, 3);
    assert.equal(e.testers, 15);
    assert.equal(e.completed, 12);
    const bad = buildPublishedEntry({ name: "X", packageName: "x;y" }, {}, []);
    assert.equal(bad.packageName, "");
    assert.equal(bad.rating, null);
  });
});
