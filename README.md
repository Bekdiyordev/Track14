# Track14

Android app where developers help each other pass Google Play's closed test
(12 testers x 14 days). Kotlin + Jetpack Compose + Firebase. Currency: **Coin14**.

- Plan and design: see the project plan document and the UI canvas.
- Architecture: [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
- Security: [docs/SECURITY.md](docs/SECURITY.md)

## Modules

| Module | Kind | Purpose |
| --- | --- | --- |
| `:domain` | pure Kotlin (JVM) | models, business rules, repository contracts. No Android, no Firebase. |
| `:app` | Android application | UI (Compose), DI (Hilt), Firebase data layer. |
| `firebase/` | Firebase config | Firestore security rules, indexes, emulator config. |
| `firebase/functions` | Cloud Functions (TypeScript, Node 22) | coins, tests, daily checks, catalog. The only writer of money and test state. |

## Build

Open the folder in Android Studio and sync. Unit tests:

    ./gradlew :domain:test :app:testDebugUnitTest

Debug builds use the App Check debug provider: run the app once, copy the debug
token from Logcat (`DebugAppCheckProvider`) and register it in the Firebase console.

## Firebase

Functions: `cd firebase/functions && npm install && npm test` (pure rules, no emulator needed).

| Function | Kind | What it does |
| --- | --- | --- |
| `onUserCreated` | Auth trigger | creates the wallet, pays the welcome bonus once |
| `createCampaign` | callable | owner pays the listing fee, app goes live |
| `onCampaignWritten`, `onAppWritten` | Firestore triggers | keep the public `catalog/{appId}` in sync |
| `startTest` | callable | tester joins (one test per app, max active tests) |
| `verifyDay` | callable | records today's 30 s check, pays the day, completes day 14 |
| `leaveTest` | callable | leave with the penalty |
| `dailyRollover` | hourly schedule | marks missed days, fails or completes tests |

Every coin movement goes through `ledger.ts`: the `transactions/{key}` id is an idempotency key.

`app/google-services.json` belongs to project `track14-c2822`. Deploy rules with
`firebase deploy --only firestore --project track14-c2822` from the `firebase/` folder.
