import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { shortName, testerSnapshot } from "../tests";

const doc = (data: Record<string, unknown>) => ({ get: (f: string) => data[f] });

describe("tester snapshot", () => {
  it("shows first name and last initial only", () => {
    assert.equal(shortName("Sardor Karimov"), "Sardor K.");
    assert.equal(shortName("  Ali  "), "Ali");
    assert.equal(shortName("Anna Maria von berg"), "Anna B.");
    assert.equal(shortName(null), "");
  });
  it("copies device and reliability, never email", () => {
    const s = testerSnapshot(doc({ displayName: "Sardor Karimov", email: "x@y.z", deviceModel: "Pixel 7", stats: { testsCompleted: 9, testsFailed: 1 } }));
    assert.deepEqual(s, { testerName: "Sardor K.", testerDevice: "Pixel 7", testerReliability: 90, testerCompleted: 9 });
    assert.equal(JSON.stringify(s).includes("x@y.z"), false);
  });
});
