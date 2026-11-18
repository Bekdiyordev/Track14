import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { nextStatus } from "../campaigns";

describe("owner status transitions", () => {
  it("pause and resume", () => {
    assert.equal(nextStatus("active", "pause"), "paused");
    assert.equal(nextStatus("paused", "resume"), "active");
    assert.equal(nextStatus("paused", "pause"), null);
    assert.equal(nextStatus("active", "resume"), null);
  });
  it("publish from active or paused only", () => {
    assert.equal(nextStatus("active", "publish"), "published");
    assert.equal(nextStatus("paused", "publish"), "published");
    assert.equal(nextStatus("published", "publish"), null);
    assert.equal(nextStatus("draft", "publish"), null);
    assert.equal(nextStatus("suspended", "resume"), null);
  });
});
