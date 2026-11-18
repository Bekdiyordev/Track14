import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { isStaff } from "../guards";

describe("staff roles", () => {
  it("only the three staff roles bypass maintenance", () => {
    assert.equal(isStaff("superadmin"), true);
    assert.equal(isStaff("support"), true);
    assert.equal(isStaff("admin"), false);
    assert.equal(isStaff(undefined), false);
  });
});
