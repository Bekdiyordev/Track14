import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { campaignStatusAfterOwnerDeleted, closesTest } from "../account";

describe("account deletion", () => {
  it("closes running campaigns and keeps finished ones", () => {
    assert.equal(campaignStatusAfterOwnerDeleted("active"), "removed");
    assert.equal(campaignStatusAfterOwnerDeleted("paused"), "removed");
    assert.equal(campaignStatusAfterOwnerDeleted("published"), null);
    assert.equal(campaignStatusAfterOwnerDeleted("completed"), null);
  });
  it("ends only running tests", () => {
    assert.equal(closesTest("active"), true);
    assert.equal(closesTest("joining"), true);
    assert.equal(closesTest("completed"), false);
    assert.equal(closesTest("failed"), false);
  });
});
