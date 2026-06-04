# Track14 Product and Implementation Plan

Last updated: 2026-06-03
Current step: `STEP-01` - Project Foundation in progress

## 1. Product Goal

Track14 helps Android developers organize the Google Play closed testing process. The app lets users discover apps that need testers, join tests, track daily 14-day testing streaks, and earn ranking points by testing other users' apps.

Important policy context:

- Google Play documentation says new personal developer accounts created after 2023-11-13 need a closed test before production access.
- The closed test must have at least 12 opted-in testers for 14 continuous days before applying for production access.
- Track14 can help organize testers and engagement, but it cannot guarantee Google Play production approval.

Sources:

- https://support.google.com/googleplay/android-developer/answer/14151465
- https://support.google.com/googleplay/android-developer/answer/9845334
- https://support.google.com/googleplay/android-developer/answer/10158779
- https://developer.android.com/training/package-visibility/declaring

## 1.1 Confirmed Product Decisions

Confirmed on 2026-06-03:

- Auth: email + password for MVP.
- Backend: Firebase is accepted as the default MVP backend.
- App adding: the owner selects an already installed app from the device, not an APK file from storage.
- App metadata: app label, icon, and package name are read from the selected installed app.
- Package visibility: MVP should avoid `QUERY_ALL_PACKAGES` unless it becomes absolutely required and is declared in Play Console. Track14 should first list launchable user-facing apps with the narrowest package visibility approach.
- Google Group step: tapping/opening the Google Group link is enough to mark that step as joined inside Track14.
- Install/play step: if the Play install or open step does not work, show clear info telling the user to join the Google Group again and retry the Play test link.
- Daily test pass rule: after Start Test, Track14 opens the target app. If the user returns to Track14 before 30 seconds, the day is not completed. If the user stays away for at least 30 seconds, the daily test passes.
- Timezone: use the user's local timezone for day boundaries in the UI, while storing UTC timestamps and timezone ID.
- Language: first UI should be Uzbek by default.
- Scoring: use the optimized MVP scoring formula in the Ranking Rules section.

## 2. Core Users

### App Owner

Developer who adds their Android app to Track14 and needs testers.

Main needs:

- Add an app quickly.
- Share Google Group, Play test opt-in, and Play Store/install links.
- See who joined the test.
- See each tester's 14-day streak.
- Finish or reject testers when needed.
- Improve home ranking by testing other apps.

### Tester

User who joins other developers' apps and tests them daily.

Main needs:

- Browse apps that need testers.
- Understand how to join a Google Group and Play closed test.
- Start daily testing easily.
- See which days are completed, missed, or pending.
- Leave a joined test when needed.

## 3. MVP Scope

### Included In MVP

- Public app browsing without authentication.
- Email authentication only when an important action is started.
- Bottom navigation with 4 tabs:
  - Home
  - Testing Apps
  - My Apps
  - Profile
- Home list sorted by ranking score.
- App detail screen with Google Group join, Play opt-in, and install/open actions.
- Testing Apps list for apps joined by the current user.
- 14-day streak box per joined app.
- Daily test flow:
  - User taps Start Test.
  - Track14 opens the target app by package name.
  - If the user stays away from Track14 for at least 30 seconds, the day becomes eligible.
  - When the user returns, Track14 marks the day complete if the 30-second rule passed.
  - If the user returns before 30 seconds, the day remains incomplete.
  - A local notification reminds the user to return after the 30-second window.
- My Apps:
  - Add own app.
  - Free users can add up to 3 apps.
  - Select an already installed app from the device.
  - Read app name, package name, and icon from the selected installed app when possible.
  - Manual fallback fields if installed-app metadata cannot be read.
  - Show owner app list.
  - Show testers and their streak boxes.
  - Finish or reject tester from Track14 status.
- Profile:
  - Theme setting.
  - Support.
  - Join Track14 Google Group.
  - Community link.
  - Auth/account actions.
- Basic points system:
  - Valid daily test gives points.
  - Missed day subtracts points.
  - Leaving a joined test subtracts points.
  - User points help rank their own active apps higher on Home.

### Not Included In First MVP

- Paid plans beyond showing the 3-app free limit.
- Automatic verification that a user actually joined a Google Group.
- Automatic removal of testers from Google Groups.
- Full Play Console API integration.
- Complex anti-fraud engine.
- Public comments/reviews.
- Multi-language UI beyond the first chosen language.

## 4. Key Product Rules

### Authentication Rules

Browsing is public.

Authentication is required for:

- Joining an app test.
- Starting a daily test.
- Adding an app.
- Viewing My Apps.
- Finishing or rejecting testers.
- Leaving a joined test.
- Updating profile settings.

Recommended MVP auth: Firebase Authentication with email sign-in.

Selected MVP method: email + password.

### App Joining Rules

An app owner must provide:

- App name, read from the selected installed app when possible.
- Package name, read from the selected installed app when possible.
- App icon, read from the selected installed app when possible.
- Google Group join URL.
- Play closed test opt-in URL.
- Play install URL, or enough package data to generate `https://play.google.com/store/apps/details?id={packageName}`.

Installed-app selection rules:

- Show launchable user-facing installed apps.
- Prefer narrow Android package visibility declarations.
- Do not use `QUERY_ALL_PACKAGES` in MVP unless there is no workable alternative and the Play Console permission declaration is prepared.
- Provide manual fallback for package name, app name, and icon if device/package visibility limits block metadata access.

Tester flow:

1. Open app detail.
2. Tap Join Google Group.
3. Track14 opens the Google Group URL.
4. Track14 marks the Google Group step complete after the link is opened.
5. Track14 opens Play test opt-in URL.
6. User installs or opens the app.
7. Track14 creates a joined test record.

If the Play test or install step does not work, Track14 should show a clear message:

- Re-check that the Google account joined the Google Group.
- Re-open the Google Group link if needed.
- Re-open the Play test opt-in link.

Note: Track14 cannot reliably verify Google Group membership from the Android client. MVP uses link-open tracking plus owner-side visibility.

### 14-Day Streak Rules

Each joined app has a 14-day streak grid.

Day states:

- `pending`: future day or not started yet.
- `completed`: user passed the daily 30-second test.
- `missed`: user did not complete the day.
- `rejected`: owner rejected this tester.
- `finished`: owner marked testing complete.
- `left`: tester left the test.

Counting:

- Day 1 starts on the join date.
- One completed test per app per day is enough.
- Missing a day marks that day red.
- A missed day affects score, but the UI still shows the full 14-day history.

Recommended MVP: use the user's local timezone for UI, and store UTC timestamps plus timezone ID in the database.

### Daily Test Rules

Daily test flow:

1. User taps Start Test.
2. Track14 stores `startedAt`.
3. Track14 launches the target app using the package name.
4. Track14 schedules a local notification for about 30 seconds later.
5. When user returns to Track14:
   - If elapsed time is at least 30 seconds, mark today's test completed.
   - If elapsed time is less than 30 seconds, keep it incomplete.
6. If a 30-second local notification/alarm fires while Track14 is still in the background, the notification can tell the user that today's test is eligible/completed and they can return.

Technical caveat:

- Android may delay background work and notifications. The authoritative check is still the elapsed time between Start Test and the user's return to Track14.

### Ranking Rules

Proposed MVP formula:

- `+2` points for each valid daily test on another user's app.
- `-1` point for each missed joined-test day.
- `-5` points for leaving a joined test before day 14.
- `0` points for testing your own app.
- No points for only opening a Google Group link or only joining a test.
- Home ranking uses owner score, app freshness, and tester need.

Recommended ranking formula:

- `rankingScore = ownerScore + freshnessBoost + testerNeedBoost`
- `freshnessBoost`: temporary small boost for newly added active apps.
- `testerNeedBoost`: small boost for apps that still need more testers.
- Owner score remains the main ranking factor.

### Owner Control Rules

App owner can:

- See tester list for their app.
- See each tester's 14-day streak.
- Mark tester as `finished`.
- Mark tester as `rejected`.
- See tester's public joined/testing stats.

Important limitation:

- Finish/reject changes Track14 status only. It does not automatically remove a tester from Google Group or Play Console.

## 5. Main Screens

### Home

Purpose: Discover apps that need testers.

Content:

- Search field.
- App list sorted by ranking score.
- App card:
  - Icon.
  - Name.
  - Package name.
  - Owner score.
  - Tester count.
  - Remaining tester need.
  - Join/Test status badge.
- Empty state if no apps exist.

Primary actions:

- Open app detail.
- Join test.

### App Detail

Purpose: Explain and start the test joining process.

Content:

- App icon, name, package name.
- Owner info.
- Current tester count.
- Step-by-step instructions:
  - Join Google Group.
  - Open Play opt-in link.
  - Install/open app.
  - Return to Track14.
- Buttons:
  - Join Google Group.
  - Open Play Test.
  - Install/Open App.
  - Retry Join/Play links if install is not available.

### Testing Apps

Purpose: Track apps the user joined.

Content:

- Joined app list.
- Streak preview.
- Today's status.
- Start Test button.
- Leave Test action.

App detail inside Testing Apps:

- Full 14-day streak grid.
- Today's action.
- Last test time.
- Missed days.
- Points gained/lost.

### My Apps

Purpose: Manage apps uploaded by current user.

Content:

- Add App button.
- Free plan counter: `0/3`, `1/3`, etc.
- Owner app list.
- App status:
  - active
  - paused
  - completed

Add app flow:

1. Explain required links and why they are needed.
2. Select an installed app from the device.
3. Read app name, icon, and package name when possible.
4. Fill Google Group URL.
5. Fill Play test opt-in URL.
6. Review and publish to Track14 Home.

Owner app detail:

- App metadata.
- Tester list.
- Tester streak boxes.
- Tester actions:
  - Finish
  - Reject
- Owner instructions for managing Google Group/Play Console manually.

### Profile

Purpose: Account and app settings.

Content:

- Sign in/out.
- Email.
- Owner score.
- Theme:
  - System
  - Light
  - Dark
- Support link.
- Join Track14 Google Group.
- Community link.
- App version.

## 6. Suggested Technical Architecture

Current project state:

- Android app.
- Kotlin.
- Jetpack Compose.
- Material 3.
- No navigation, auth, backend, or data layer yet.

Recommended MVP stack:

- UI: Jetpack Compose + Material 3.
- Navigation: AndroidX Navigation Compose.
- Auth: Firebase Authentication email sign-in.
- Database: Cloud Firestore.
- Storage: Firebase Storage for app icons when copied from selected installed-app metadata.
- Notifications: Local Android notifications for the 30-second reminder.
- Background scheduling: WorkManager where needed.
- Installed app metadata: Android `PackageManager`, limited to launchable user-facing apps for MVP.
- Dependency injection: Hilt only if complexity grows; otherwise start with simple repositories.
- Local state: DataStore for theme and lightweight preferences.

Selected MVP backend: Firebase.

## 7. Data Model Draft

### `users`

Fields:

- `id`
- `email`
- `displayName`
- `photoUrl`
- `ownerScore`
- `freeAppLimit`
- `themeMode`
- `timezoneId`
- `createdAt`
- `updatedAt`

### `apps`

Fields:

- `id`
- `ownerId`
- `name`
- `packageName`
- `iconUrl`
- `googleGroupUrl`
- `playOptInUrl`
- `playInstallUrl`
- `description`
- `status`
- `testerCount`
- `requiredTesterCount`
- `rankingScore`
- `createdAt`
- `updatedAt`

### `testMemberships`

Fields:

- `id`
- `appId`
- `testerId`
- `ownerId`
- `status`
- `joinedAt`
- `leftAt`
- `finishedAt`
- `rejectedAt`
- `timezoneId`
- `completedDaysCount`
- `missedDaysCount`
- `pointsDelta`

### `dailyTests`

Fields:

- `id`
- `membershipId`
- `appId`
- `testerId`
- `testDate`
- `status`
- `startedAt`
- `eligibleAt`
- `completedAt`
- `elapsedSeconds`
- `pointsApplied`

### `scoreEvents`

Fields:

- `id`
- `userId`
- `sourceType`
- `sourceId`
- `points`
- `reason`
- `createdAt`

## 8. Implementation Steps

Use these IDs when asking "which step are we on?"

### `STEP-00` Product Plan

Status: completed

Deliverables:

- Product rules defined.
- MVP scope defined.
- Technical architecture proposed.
- Open decisions listed.

Acceptance criteria:

- `TRACK14_PLAN.md` exists.
- Next coding step is clear.

### `STEP-01` Project Foundation

Status: in progress

Tasks:

- Add Navigation Compose.
- Add screen package structure.
- Add app-level route model.
- Add Material 3 app shell.
- Add bottom navigation with Home, Testing Apps, My Apps, Profile.
- Replace default `Greeting` screen.

Acceptance criteria:

- App opens with 4-tab bottom nav.
- Each tab has a placeholder screen.
- No auth/backend required yet.
- Project builds.

### `STEP-02` Theme And UI Foundation

Status: not started

Tasks:

- Define Track14 color tokens.
- Add light/dark/system theme support.
- Add reusable UI components:
  - AppCard
  - StreakGrid
  - EmptyState
  - SectionHeader
  - PrimaryActionButton
- Add preview data.

Acceptance criteria:

- UI has consistent spacing and typography.
- StreakGrid supports pending/completed/missed states.
- Profile can show theme options, even before persistence.

### `STEP-03` Domain Models And Fake Repository

Status: not started

Tasks:

- Create Kotlin domain models:
  - TrackApp
  - UserProfile
  - TestMembership
  - DailyTest
  - ScoreEvent
- Add fake in-memory repository for UI development.
- Add sample apps and sample streaks.

Acceptance criteria:

- Home renders fake app list.
- Testing Apps renders fake joined apps.
- My Apps renders fake owner apps.
- Owner tester streak view can be mocked.

### `STEP-04` Home And App Detail Flow

Status: not started

Tasks:

- Build Home list.
- Sort apps by ranking score.
- Build app detail screen.
- Add instruction section for joining Google Group and Play test.
- Add external link opening.

Acceptance criteria:

- User can browse without auth.
- User can open app detail.
- Join flow shows clear steps.

### `STEP-05` Auth Gate

Status: not started

Tasks:

- Add auth abstraction.
- Add email sign-in UI.
- Require auth for important actions.
- Keep public browsing available.

Acceptance criteria:

- Home remains public.
- Add app, join test, start test, and profile account actions request auth.
- Signed-in state is reflected in Profile.

### `STEP-06` Backend Integration

Status: not started

Tasks:

- Connect selected backend.
- Implement users, apps, memberships, daily tests, and score events.
- Add repository interfaces.
- Replace fake repository gradually.

Acceptance criteria:

- App list loads from backend.
- Signed-in user can create membership.
- My Apps loads only current user's apps.

### `STEP-07` Add Own App Flow

Status: not started

Tasks:

- Add installed app picker.
- List launchable user-facing installed apps.
- Extract app label, package name, and icon with `PackageManager` when possible.
- Add manual fallback for package/name/icon.
- Avoid `QUERY_ALL_PACKAGES` unless Play policy declaration is approved.
- Validate Google Group and Play opt-in URLs.
- Enforce free 3-app limit.
- Save app to backend.

Acceptance criteria:

- User can add an app.
- App appears in My Apps.
- App appears on Home when active.
- Limit prevents more than 3 free apps.

### `STEP-08` Join Test Flow

Status: not started

Tasks:

- Implement Google Group link step.
- Implement Play opt-in link step.
- Mark Google Group step complete after the link is opened.
- Show retry/help info if Play install is unavailable.
- Create membership record.
- Show joined app in Testing Apps.

Acceptance criteria:

- User can join another user's app.
- Joined app appears in Testing Apps.
- Duplicate join is prevented.
- User cannot join own app for points.

### `STEP-09` Daily Test And 14-Day Streak

Status: not started

Tasks:

- Implement Start Test.
- Launch target app by package name.
- Track return elapsed time.
- Mark daily test complete after at least 30 seconds.
- Mark missed days.
- Schedule local notification reminder.
- Update streak UI.

Acceptance criteria:

- One successful daily test checks today's box.
- Returning before 30 seconds does not complete the day.
- Staying outside Track14 for at least 30 seconds completes the day when the app confirms elapsed time.
- Missed days are shown red.
- Only one completion per app per day counts.

### `STEP-10` Scoring And Ranking

Status: not started

Tasks:

- Apply score events for completed, missed, and left tests.
- Recalculate user owner score.
- Recalculate app ranking score.
- Sort Home by ranking score.

Acceptance criteria:

- Testing other apps increases score.
- Missing/leaving decreases score.
- Own apps move in Home according to owner score.

### `STEP-11` Owner Tester Management

Status: not started

Tasks:

- Build owner app detail.
- Show tester list.
- Show each tester's streak box.
- Add finish tester action.
- Add reject tester action.

Acceptance criteria:

- Owner sees testers for each own app.
- Owner can finish/reject testers in Track14.
- Tester membership status updates.

### `STEP-12` Profile And Settings

Status: not started

Tasks:

- Persist theme setting.
- Add support link.
- Add Track14 Google Group link.
- Add community link.
- Add app version display.

Acceptance criteria:

- Theme changes apply.
- Profile contains required links and account actions.

### `STEP-13` QA And Release Prep

Status: not started

Tasks:

- Add unit tests for scoring and streak calculation.
- Add UI tests for main navigation.
- Test add app flow.
- Test daily test flow on emulator/device.
- Check permissions and notification behavior.
- Prepare release checklist.

Acceptance criteria:

- Project builds.
- Core flows tested.
- Known limitations documented.

## 9. Remaining Open Items

These can be filled later without blocking `STEP-01` through `STEP-04`:

1. Track14 support URL or support email.
2. Track14 Google Group URL.
3. Track14 community URL.
4. App branding details: final logo, launcher icon, and app description.
5. Whether user profile should show public display name, email prefix, or anonymous tester ID.

## 10. Working Rule For Future Steps

Before starting each coding step:

1. Update `Current step` at the top of this file.
2. Mark the step status as `in progress`.
3. Implement only that step's scope unless a dependency is required.
4. Build or test the project.
5. Mark the step `completed` when acceptance criteria pass.
6. If blocked, write the blocker under that step.
