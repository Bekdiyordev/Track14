/**
 * Pure business rules. Mirror of :domain (StreakRules, DailyCheckRules) on the Android side;
 * the server version is the source of truth. Keep both in sync and covered by tests.
 */
export const TEST_DAYS = 14;
export const MIN_PASSED_TO_COMPLETE = 12;
export const MAX_MISSED_IN_ROW = 1;
export const MAX_MISSED_TOTAL = 2;
export const MIN_FOREGROUND_SECONDS = 30;

export type DayStatus = "pending" | "passed" | "missed";
export type Outcome = "active" | "completed" | "failed";

/** 1 missed day is tolerated; 2 in a row or 3 in total fail; day 14 needs at least 12 green. */
export function evaluate(days: readonly DayStatus[]): Outcome {
  if (days.length > TEST_DAYS) throw new Error(`at most ${TEST_DAYS} days, got ${days.length}`);
  let missedTotal = 0;
  let missedInRow = 0;
  let passed = 0;
  let pending = false;
  for (const day of days) {
    if (day === "passed") {
      passed++;
      missedInRow = 0;
    } else if (day === "missed") {
      missedTotal++;
      missedInRow++;
      if (missedInRow > MAX_MISSED_IN_ROW || missedTotal > MAX_MISSED_TOTAL) return "failed";
    } else {
      pending = true;
    }
  }
  if (days.length < TEST_DAYS || pending) return "active";
  return passed >= MIN_PASSED_TO_COMPLETE ? "completed" : "failed";
}

export function isPassed(foregroundSeconds: number): boolean {
  return foregroundSeconds >= MIN_FOREGROUND_SECONDS;
}

export function isValidTimeZone(tz: string): boolean {
  try {
    new Intl.DateTimeFormat("en-US", { timeZone: tz });
    return true;
  } catch {
    return false;
  }
}

/** Calendar date (YYYY-MM-DD) of an instant in the tester's time zone. */
export function localDate(epochMs: number, timeZone: string): string {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date(epochMs));
}

function dateIndex(isoDate: string): number {
  const [y, m, d] = isoDate.split("-").map(Number);
  return Math.round(Date.UTC(y, m - 1, d) / 86_400_000);
}

/** 1-based test day of `nowMs`: the start date is day 1, the next calendar day is day 2, ... */
export function dayNumber(startMs: number, nowMs: number, timeZone: string): number {
  return dateIndex(localDate(nowMs, timeZone)) - dateIndex(localDate(startMs, timeZone)) + 1;
}

/** Days 1..closed are over; anything without a "passed" record among them counts as missed. */
export function closedDayStatuses(passedDays: ReadonlySet<number>, closedUpTo: number): DayStatus[] {
  const last = Math.min(closedUpTo, TEST_DAYS);
  const out: DayStatus[] = [];
  for (let n = 1; n <= last; n++) out.push(passedDays.has(n) ? "passed" : "missed");
  return out;
}

/** Firestore-safe idempotency key for the coin ledger. */
export function ledgerKey(...parts: (string | number)[]): string {
  const key = parts.join("_");
  if (!/^[A-Za-z0-9_-]{1,200}$/.test(key)) throw new Error(`invalid ledger key: ${key}`);
  return key;
}
