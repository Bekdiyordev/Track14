import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { blockEnd } from "../admin";
import { isBlockActive } from "../guards";

describe("temporary blocks", () => {
  const now = Date.UTC(2026, 10, 19);
  it("ends after 7 or 30 days, or never", () => {
    assert.equal(blockEnd(7, now)?.getTime(), now + 7 * 86_400_000);
    assert.equal(blockEnd(30, now)?.getTime(), now + 30 * 86_400_000);
    assert.equal(blockEnd(null, now), null);
  });
  it("is in force only while set and not expired", () => {
    assert.equal(isBlockActive(true, null, now), true);
    assert.equal(isBlockActive(true, now + 1, now), true);
    assert.equal(isBlockActive(true, now, now), false);
    assert.equal(isBlockActive(false, null, now), false);
    assert.equal(isBlockActive(undefined, null, now), false);
  });
});
