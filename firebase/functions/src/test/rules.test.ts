import { strict as assert } from "node:assert";
import { describe, it } from "node:test";
import { closedDayStatuses, dayNumber, DayStatus, evaluate, isPassed, isValidTimeZone, ledgerKey, localDate } from "../rules";

const P: DayStatus = "passed";
const M: DayStatus = "missed";
const fill = (head: DayStatus[], rest: DayStatus = "pending") => [...head, ...Array(14 - head.length).fill(rest)];

describe("evaluate (same cases as Android StreakRulesTest)", () => {
  it("fresh test is active", () => assert.equal(evaluate([]), "active"));
  it("one miss keeps going", () => assert.equal(evaluate(fill([P, M, P])), "active"));
  it("two in a row fail", () => assert.equal(evaluate(fill([P, M, M])), "failed"));
  it("three in total fail", () => assert.equal(evaluate(fill([M, P, M, P, M])), "failed"));
  it("two separated misses tolerated", () => assert.equal(evaluate(fill([M, P, M, P])), "active"));
  it("14 green complete", () => assert.equal(evaluate(Array(14).fill(P)), "completed"));
  it("12 green + 2 scattered misses complete", () => assert.equal(evaluate(fill([P, M, P, P, M], P)), "completed"));
  it("pending day keeps it active", () => assert.equal(evaluate([...Array(13).fill(P), "pending"]), "active"));
  it("more than 14 days throws", () => assert.throws(() => evaluate(Array(15).fill(P))));
});

describe("daily check", () => {
  it("30 seconds is the minimum", () => {
    assert.equal(isPassed(29), false);
    assert.equal(isPassed(30), true);
  });
});

describe("time zones and day numbers", () => {
  const tz = "Asia/Samarkand"; // UTC+5
  it("validates time zones", () => {
    assert.equal(isValidTimeZone(tz), true);
    assert.equal(isValidTimeZone("Mars/Olympus"), false);
  });
  it("local date follows the tester's zone", () => {
    // 2026-11-16 20:00 UTC is already 17 November in Samarkand.
    assert.equal(localDate(Date.UTC(2026, 10, 16, 20), tz), "2026-11-17");
    assert.equal(localDate(Date.UTC(2026, 10, 16, 20), "UTC"), "2026-11-16");
  });
  it("start day is day 1, local midnight starts day 2", () => {
    const start = Date.UTC(2026, 10, 16, 15); // 20:00 local
    assert.equal(dayNumber(start, start, tz), 1);
    assert.equal(dayNumber(start, Date.UTC(2026, 10, 16, 18, 59), tz), 1); // 23:59 local
    assert.equal(dayNumber(start, Date.UTC(2026, 10, 16, 19, 0), tz), 2); // 00:00 local
    assert.equal(dayNumber(start, Date.UTC(2026, 10, 29, 12), tz), 14);
    assert.equal(dayNumber(start, Date.UTC(2026, 10, 30, 12), tz), 15);
  });
});

describe("closed days", () => {
  it("unrecorded closed days are missed and capped at 14", () => {
    assert.deepEqual(closedDayStatuses(new Set([1, 3]), 3), [P, M, P]);
    assert.equal(closedDayStatuses(new Set(), 20).length, 14);
    assert.deepEqual(closedDayStatuses(new Set([1]), 0), []);
  });
});

describe("ledger keys", () => {
  it("accepts safe keys, rejects paths", () => {
    assert.equal(ledgerKey("camp_uid", 3), "camp_uid_3");
    assert.throws(() => ledgerKey("a/b"));
    assert.throws(() => ledgerKey("x", "../wallets"));
  });
});
