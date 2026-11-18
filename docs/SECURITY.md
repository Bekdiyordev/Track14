# Security

## Trust model

The phone is untrusted. Anything that moves value or proves a test day is decided on the server.

| Concern | Control |
| --- | --- |
| Coin forgery | `wallets`, `transactions`, `tests`, `campaigns`, `purchases`, `catalog` are `allow write: if false`; only Cloud Functions (Admin SDK) write them. |
| Fake test days | Day status and seconds are computed by a Function; the client value is never trusted (30 s rule re-checked server side). |
| Cloned or modified app | Firebase App Check: Play Integrity in release, debug provider in debug. Enforce it in the console for Firestore and Functions. |
| Duplicate accounts | `devices/{deviceId}` written by a Function with Play Integrity verdicts. |
| Data exposure | Public reads only for `catalog` and `config`. Owner data is never in `catalog`. |
| Staff access | Custom claim `role` = superadmin / moderator / support. Admin actions are callable Functions that write `auditLogs`. |
| Blocked users | `users/{uid}.blocked` is written only by a Function. Writes by blocked users are rejected in rules. |
| Transport | HTTPS only: `usesCleartextTraffic=false` and a network security config without user CAs. |
| Local data | `allowBackup=false`, cloud backup and device transfer excluded. No secrets in the app. |
| Release hardening | R8 shrinking and obfuscation enabled for release. |
| Purchases | Play Billing; `verifyPurchase` validates the token server side, `purchases/{token}` makes it idempotent. |

## Rules for contributors

- Never add a client write to a server-owned collection; add a Function instead.
- Never log tokens, emails or coin balances.
- Every Function that writes coins uses an idempotency key (for example `testId_day`).
- Validate every callable input; reject unknown fields.

## Before launch

- Enforce App Check; test with the debug token disabled in release.
- Run the rules against the emulator with allow and deny cases for every collection.
- Store the upload keystore outside the repo and enable Play App Signing.

## Daily check and device data

- `PACKAGE_USAGE_STATS` is used only to read the foreground time of the apps under test
  (`core/device/UsageAccess.kt`). Play Console needs a permission declaration and a privacy
  policy section for it before launch.
- Fallback mode (no permission) measures the time between opening the app from Track14 and
  coming back; the server stores the mode, so owners and admins can tell the two apart.
- Owner-provided links are untrusted: only `https://play.google.com/apps/testing/<package>`
  and Play Store pages are opened (`PlayLinks`), checked on the server and on the client.
- The day number is computed on the server from its own clock and the test's time zone.
