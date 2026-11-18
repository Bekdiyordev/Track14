import { getFirestore } from "firebase-admin/firestore";

export const DEFAULT_TESTERS_NEEDED = 12;

/** Starting values (plan, section 5). Real values: Firestore `config/economy`, tuned at the end. */
export interface Economy {
  standardFee: number;
  proFee: number;
  standardRewardPerDay: number;
  proRewardPerDay: number;
  completionBonus: number;
  feedbackReward: number;
  failedPenalty: number;
  leftPenalty: number;
  /** Enough for at least three Standard listings (decision in the plan). */
  welcomeBonus: number;
  maxActiveTests: number;
  testersNeeded: number;
  /** Top placement for 1 / 3 / 7 days (plan: 150 / 350 / 700). */
  feature1Price: number;
  feature3Price: number;
  feature7Price: number;
}

export const DEFAULT_ECONOMY: Economy = {
  standardFee: 1000,
  proFee: 1800,
  standardRewardPerDay: 3,
  proRewardPerDay: 5,
  completionBonus: 10,
  feedbackReward: 2,
  failedPenalty: 10,
  leftPenalty: 15,
  welcomeBonus: 3000,
  maxActiveTests: 15,
  testersNeeded: DEFAULT_TESTERS_NEEDED,
  feature1Price: 150,
  feature3Price: 350,
  feature7Price: 700,
};

export const featurePrice = (e: Economy, days: 1 | 3 | 7) =>
  days === 1 ? e.feature1Price : days === 3 ? e.feature3Price : e.feature7Price;

export type Plan = "standard" | "pro";

export const fee = (e: Economy, plan: Plan) => (plan === "pro" ? e.proFee : e.standardFee);
export const rewardPerDay = (e: Economy, plan: Plan) =>
  plan === "pro" ? e.proRewardPerDay : e.standardRewardPerDay;

/** Merges the stored config over the defaults, ignoring anything that is not a non-negative integer. */
export function mergeEconomy(stored: Record<string, unknown> | undefined): Economy {
  const out: Economy = { ...DEFAULT_ECONOMY };
  if (!stored) return out;
  for (const key of Object.keys(DEFAULT_ECONOMY) as (keyof Economy)[]) {
    const v = stored[key];
    if (typeof v === "number" && Number.isInteger(v) && v >= 0) out[key] = v;
  }
  return out;
}

let cache: { value: Economy; at: number } | null = null;
const TTL_MS = 60_000;

export async function loadEconomy(): Promise<Economy> {
  if (cache && Date.now() - cache.at < TTL_MS) return cache.value;
  const snap = await getFirestore().doc("config/economy").get();
  cache = { value: mergeEconomy(snap.data()), at: Date.now() };
  return cache.value;
}
