# Firebase setup status (20 November 2026)

Project: `track14-c2822`. Both Android apps are registered, their debug SHA-1 and
SHA-256 fingerprints are added, and Google sign-in is enabled. Downloaded SDK
configurations and the real Web client ID are included in both modules.

- Firestore `(default)` exists in `europe-central2`; production security rules and
  all 12 composite indexes have been deployed. The four starting `config` documents
  have been created without overwriting existing data.
- App Check terms have been accepted. Samsung QA debug tokens for both apps have
  been registered; their secret values stay outside Git. Play Integrity provider
  registration and API enforcement still need completion and device verification.
- Billing is intentionally deferred at the owner's request. Cloud Functions and
  the `auditLogs.expireAt` TTL policy are **not deployed**. Full backend flows,
  purchases, account deletion and scheduled work cannot be validated yet.
- The first superadmin is approved for `abekdiyor@gmail.com`. That Google account
  must first complete Firebase sign-in before the role can be assigned. Samsung
  QA currently encounters connection timeouts to Google Auth/App Check on Wi-Fi
  (IPv4 TCP succeeds; IPv6 TCP times out); no successful Firebase login is claimed.
- Both debug APKs build, unit tests and lint pass. The admin login now displays
  errors and permits retry; Firebase sign-in stops waiting after 30 seconds in both
  apps. User profile setup does not keep the main login busy while offline.

Resume with working device connectivity, finish App Check, assign the approved
role and test both apps. Once billing is connected, deploy Firestore including TTL
and Functions, then verify the server-dependent flows before release. Existing
Auth users created before `onUserCreated` is deployed will need an idempotent
welcome-wallet backfill; the Auth creation trigger does not replay old users.

# Setup: Google sign-in

The following describes setup for a new project or signing key. The current debug
configuration is already complete for both `:app` and `:admin`:

1. Firebase console > Authentication > Sign-in method > enable **Google**.
2. Get the debug SHA-1 and add it in Project settings > Your apps > Android > Add fingerprint:

       keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

   Add the release/upload key SHA-1 and the Play App Signing SHA-1 later, before launch.
3. Download `google-services.json` again and replace `app/google-services.json`.
4. Authentication > Sign-in method > Google > Web SDK configuration: copy the **Web client ID**
   into both modules' `src/main/res/values/auth_config.xml` (`google_web_client_id`).
5. App Check: run a debug build once, copy the token from Logcat and register it under App Check > Apps > Debug tokens.

# Setup: Firebase backend (once, in the console / terminal)

6. Upgrade the project to the **Blaze** plan (Cloud Functions and schedules need it). Set a budget alert.
7. Create the Firestore database in **europe-central2** (Warsaw), the same region as the Functions.
8. Install the CLI and log in: `npm i -g firebase-tools && firebase login`.
9. Deploy from the `firebase/` folder:

       cd firebase/functions && npm install && npm test && cd ..
       firebase deploy --only firestore,functions --project track14-c2822

10. App Check > enforce for Firestore once valid app tokens are verified. Callable
    Functions already require App Check outside the local emulator in code.

# Setup: Coin14 purchases (Play Billing)

11. Play Console > Monetize > In-app products: create `coins_500`, `coins_1000`, `coins_2000`,
    `coins_5000`, `coins_10000` (one-time, consumable) and set prices. Products appear in the app
    only after a build is uploaded to a testing track.
12. Google Cloud console (project track14-c2822): enable **Google Play Android Developer API**.
13. Play Console > Users and permissions: invite the Functions service account
    `282255323635-compute@developer.gserviceaccount.com` with "View financial data" and
    "Manage orders and subscriptions".
14. Real-time notifications: create Pub/Sub topic `play-rtdn`, grant
    `google-play-developer-notifications@system.gserviceaccount.com` the Publisher role, then set
    the topic in Play Console > Monetization setup. Refunds then take the Coin14 back automatically.
15. Add your Google account as a license tester to buy without being charged.

# Setup: admin access

16. First superadmin (once, from this computer):

        gcloud auth application-default login
        cd firebase/functions && npm install && npm run build
        node lib/scripts/setRole.js you@gmail.com superadmin

    Sign out and in again in the app so the new role reaches the token. Further roles are
    given from the admin app (`adminSetRole`).

# Setup: admin app (`:admin` module)

The admin app lives in this project as a separate Android module with its own package
`uz.beko404.track14.admin`. It uses the same Firebase project; every action goes through the
`admin*` Cloud Functions, which check the staff role on the server.

17. The Android app `uz.beko404.track14.admin` is registered in `track14-c2822` as
    `1:282255323635:android:e32237d56005d8fe76e286`; its downloaded configuration is
    `admin/google-services.json`. The current development machine's debug SHA-1 and SHA-256
    are registered. Add the release signing certificates before distributing a release build.
18. Put the same Web client ID as the user app into `admin/src/main/res/values/auth_config.xml`.
19. App Check > Apps: register the admin app with Play Integrity; in debug, add the debug token
    printed in Logcat (`DebugAppCheckProvider`).
20. Deploy the new indexes: `firebase deploy --only firestore:indexes` (apps and reports by status).
21. In Android Studio choose the `admin` run configuration and sign in with the superadmin account
    from step 16.

# Setup: config, web pages, store listing

22. Starting config (once, after the first functions deploy):

        cd firebase/functions && npm run build && node lib/scripts/seedConfig.js

    Writes `config/app_version`, `support`, `flags`, `economy` only where missing
    (`--force` overwrites). Later changes go through the admin app.
23. Web pages (contact e-mail abekdiyor@gmail.com, also seeded as the support e-mail):
    `firebase deploy --only hosting`. Pages: `https://track14-c2822.web.app/privacy` (already
    linked in the app) and `/delete-account`.
24. `firebase deploy --only firestore:indexes` also turns on the TTL policy that removes audit
    entries after a year (`auditLogs.expireAt`), as the privacy policy says.
25. Play Console listing, Data safety and Usage Access declaration: copy from
    `docs/STORE_LISTING.md`.
26. Security rules tests (needs Java 11+ and `npm i -g firebase-tools`), from `firebase/`:

        npm --prefix rules-test install
        firebase emulators:exec --only firestore "npm --prefix rules-test test"

    They prove that no client, superadmin included, can write coins, tests, config or audit logs
    directly, and that profiles, apps, feedback and reports are validated.
