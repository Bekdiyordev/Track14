/**
 * Bootstrap the first superadmin (run once from your computer, not deployed):
 *
 *   gcloud auth application-default login
 *   npm run build && node lib/scripts/setRole.js you@gmail.com superadmin
 *
 * Later roles are managed from the admin app (adminSetRole). The user must sign in again
 * (or wait up to an hour) for the new claim to reach the app.
 */
import { initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";

const ROLES = ["superadmin", "moderator", "support", "none"];

async function main(): Promise<void> {
  const [email, role] = process.argv.slice(2);
  if (!email || !ROLES.includes(role)) {
    console.error(`usage: node lib/scripts/setRole.js <email> <${ROLES.join("|")}>`);
    process.exit(1);
  }
  initializeApp({ projectId: process.env.GCLOUD_PROJECT ?? "track14-c2822" });
  const user = await getAuth().getUserByEmail(email);
  await getAuth().setCustomUserClaims(user.uid, { ...(user.customClaims ?? {}), role: role === "none" ? null : role });
  console.log(`${email} (${user.uid}) -> ${role}`);
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
