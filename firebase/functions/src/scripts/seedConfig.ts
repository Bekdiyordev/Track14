/**
 * Writes the starting config documents (run once after the first deploy, from your computer):
 *
 *   gcloud auth application-default login
 *   npm run build && node lib/scripts/seedConfig.js            # only missing documents
 *   node lib/scripts/seedConfig.js --force                     # overwrite all four
 *
 * Every document is checked against the same schemas adminSetConfig uses, so the app never
 * reads a value the admin app could not have written. Later changes go through the admin app.
 */
import { initializeApp } from "firebase-admin/app";
import { FieldValue, getFirestore } from "firebase-admin/firestore";
import { CONFIG_SCHEMAS } from "../admin";
import { DEFAULT_ECONOMY } from "../economy";

export type ConfigDoc = keyof typeof CONFIG_SCHEMAS;

/** Starting values. Support links stay empty until real ones exist (the app then hides them). */
export function seedValues(): Record<ConfigDoc, Record<string, unknown>> {
  const values: Record<ConfigDoc, Record<string, unknown>> = {
    app_version: { minBuild: 1, latestBuild: 1 },
    support: { groupUrl: "https://groups.google.com/g/track14-testers", email: "abekdiyor@gmail.com" },
    flags: { maintenance: false, newApps: true, purchases: true },
    economy: { ...DEFAULT_ECONOMY },
  };
  for (const doc of Object.keys(values) as ConfigDoc[]) {
    const result = CONFIG_SCHEMAS[doc].safeParse(values[doc]);
    if (!result.success) throw new Error(`seed for config/${doc} is invalid: ${result.error.message}`);
  }
  return values;
}

async function main(): Promise<void> {
  const force = process.argv.includes("--force");
  initializeApp({ projectId: process.env.GCLOUD_PROJECT ?? "track14-c2822" });
  const db = getFirestore();
  for (const [doc, value] of Object.entries(seedValues())) {
    const ref = db.collection("config").doc(doc);
    if (!force && (await ref.get()).exists) {
      console.log(`config/${doc}: exists, skipped`);
      continue;
    }
    await ref.set({ ...value, updatedAt: FieldValue.serverTimestamp(), updatedBy: "seed" });
    console.log(`config/${doc}: written`);
  }
}

if (require.main === module) {
  main().catch((e) => {
    console.error(e);
    process.exit(1);
  });
}
