import { DEFAULT_TESTERS_NEEDED } from "./economy";

type Data = Record<string, unknown>;

/**
 * Public projection of an app with an active campaign. Contains no owner data:
 * the catalog is readable without login (see firestore.rules).
 * Returns null when the app should not be listed.
 */
export function buildCatalogEntry(app: Data | undefined, campaign: Data | undefined): Data | null {
  if (!app || !campaign) return null;
  if (campaign.status !== "active") return null;
  if (app.status === "suspended" || app.status === "removed") return null;
  if (app.ownerBlocked === true) return null;
  const name = typeof app.name === "string" ? app.name.trim() : "";
  if (!name) return null;
  const stats = (campaign.stats ?? {}) as Data;
  return {
    name: name.slice(0, 60),
    category: typeof app.category === "string" ? app.category : "",
    shortDesc: typeof app.shortDesc === "string" ? app.shortDesc.slice(0, 300) : "",
    plan: campaign.plan === "pro" ? "pro" : "standard",
    rewardPerDay: num(campaign.rewardPerDay),
    joined: num(stats.joined),
    needed: num(campaign.testersNeeded) || DEFAULT_TESTERS_NEEDED,
    rating: typeof app.rating === "number" ? app.rating : null,
    featuredUntil: campaign.featuredUntil ?? null,
    status: "active",
    createdAt: campaign.startedAt ?? null,
    packageName: isValidPackageName(app.packageName) ? app.packageName : "",
    optInUrl: isValidOptInUrl(app.optInUrl) ? app.optInUrl : "",
    completionBonus: num(campaign.completionBonus),
    ownerName: typeof campaign.ownerName === "string" ? campaign.ownerName.slice(0, 40) : "",
    ownerReliability: typeof campaign.ownerReliability === "number" ? campaign.ownerReliability : null,
    ownerTests: num(campaign.ownerTests),
  };
}

const PACKAGE = /^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z][a-zA-Z0-9_]*)+$/;
const OPT_IN_PREFIX = "https://play.google.com/apps/testing/";

/** Same checks as PlayLinks on Android: owners' links are untrusted. */
export function isValidPackageName(v: unknown): v is string {
  return typeof v === "string" && v.length <= 150 && PACKAGE.test(v);
}

export function isValidOptInUrl(v: unknown): v is string {
  if (typeof v !== "string" || !v.startsWith(OPT_IN_PREFIX)) return false;
  const pkg = v.slice(OPT_IN_PREFIX.length).split("?")[0].replace(/\/+$/, "");
  return isValidPackageName(pkg);
}

function num(v: unknown): number {
  return typeof v === "number" && Number.isFinite(v) && v > 0 ? Math.floor(v) : 0;
}
