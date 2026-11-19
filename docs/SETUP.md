# Setup: Google sign-in

`app/google-services.json` currently has an empty `oauth_client` list, so Google sign-in
is not enabled yet. Do this once:

1. Firebase console > Authentication > Sign-in method > enable **Google**.
2. Get the debug SHA-1 and add it in Project settings > Your apps > Android > Add fingerprint:

       keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

   Add the release/upload key SHA-1 and the Play App Signing SHA-1 later, before launch.
3. Download `google-services.json` again and replace `app/google-services.json`.
4. Authentication > Sign-in method > Google > Web SDK configuration: copy the **Web client ID**
   into `app/src/main/res/values/auth_config.xml` (`google_web_client_id`).
5. App Check: run a debug build once, copy the token from Logcat and register it under App Check > Apps > Debug tokens.

# Setup: Firebase backend (once, in the console / terminal)

6. Upgrade the project to the **Blaze** plan (Cloud Functions and schedules need it). Set a budget alert.
7. Create the Firestore database in **europe-central2** (Warsaw), the same region as the Functions.
8. Install the CLI and log in: `npm i -g firebase-tools && firebase login`.
9. Deploy from the `firebase/` folder:

       cd firebase/functions && npm install && npm test && cd ..
       firebase deploy --only firestore,functions --project track14-c2822

10. App Check > enforce for Firestore and Cloud Functions once the debug token works.

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

17. Firebase console > Project settings > Add app > Android: package `uz.beko404.track14.admin`,
    add the debug SHA-1 (and later the release SHA-1). Download its `google-services.json` into
    `admin/` (not `app/`).
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
23. Web pages: replace every `CONTACT_EMAIL` in `firebase/hosting/privacy/index.html` and
    `firebase/hosting/delete-account/index.html` with your support address, then
    `firebase deploy --only hosting`. Pages: `https://track14-c2822.web.app/privacy` (already
    linked in the app) and `/delete-account`.
24. `firebase deploy --only firestore:indexes` also turns on the TTL policy that removes audit
    entries after a year (`auditLogs.expireAt`), as the privacy policy says.
25. Play Console listing, Data safety and Usage Access declaration: copy from
    `docs/STORE_LISTING.md`.
