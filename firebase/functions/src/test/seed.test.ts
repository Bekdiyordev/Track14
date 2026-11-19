import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { seedValues } from "../scripts/seedConfig";

describe("config seed", () => {
  it("passes the admin schemas and starts with maintenance off", () => {
    const v = seedValues();
    assert.equal(v.flags.maintenance, false);
    assert.equal(v.economy.welcomeBonus, 3000);
    assert.deepEqual(Object.keys(v).sort(), ["app_version", "economy", "flags", "support"]);
  });
});
