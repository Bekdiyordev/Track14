package uz.beko404.track14.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import uz.beko404.track14.domain.model.AddOwnerAppRequest
import uz.beko404.track14.domain.model.AddOwnerAppResult
import uz.beko404.track14.domain.model.AuthSession
import uz.beko404.track14.domain.model.DailyTest
import uz.beko404.track14.domain.model.DailyTestResult
import uz.beko404.track14.domain.model.DailyTestStatus
import uz.beko404.track14.domain.model.JoinedTestSnapshot
import uz.beko404.track14.domain.model.JoinAppResult
import uz.beko404.track14.domain.model.OwnerTesterSnapshot
import uz.beko404.track14.domain.model.ScoreEvent
import uz.beko404.track14.domain.model.TestMembership
import uz.beko404.track14.domain.model.TestMembershipStatus
import uz.beko404.track14.domain.model.ThemeMode
import uz.beko404.track14.domain.model.Track14Snapshot
import uz.beko404.track14.domain.model.TrackApp
import uz.beko404.track14.domain.model.TrackAppStatus
import uz.beko404.track14.domain.model.UserProfile
import uz.beko404.track14.domain.repository.Track14Repository
import uz.beko404.track14.domain.rules.ScoringRules
import uz.beko404.track14.domain.rules.StreakRules

object FirebaseTrack14Repository : Track14Repository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var apps by mutableStateOf<List<TrackApp>>(emptyList())
    private var testerMemberships by mutableStateOf<List<TestMembership>>(emptyList())
    private var ownerMemberships by mutableStateOf<List<TestMembership>>(emptyList())
    private var dailyTests by mutableStateOf<List<DailyTest>>(emptyList())
    private var ownerDailyTests by mutableStateOf<List<DailyTest>>(emptyList())
    private var scoreEvents by mutableStateOf<List<ScoreEvent>>(emptyList())
    private var currentUserProfile by mutableStateOf<UserProfile?>(null)
    private var authSessionState by mutableStateOf(auth.currentUser?.toAuthSession())

    private var appsListener: ListenerRegistration? = null
    private var testerMembershipsListener: ListenerRegistration? = null
    private var ownerMembershipsListener: ListenerRegistration? = null
    private var dailyTestsListener: ListenerRegistration? = null
    private var ownerDailyTestsListener: ListenerRegistration? = null
    private var scoreEventsListener: ListenerRegistration? = null
    private var userListener: ListenerRegistration? = null

    val authSession: AuthSession?
        get() = authSessionState

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            authSessionState = user?.toAuthSession()
            currentUserProfile = user?.toDefaultUserProfile()
            listenToUser(user?.uid)
            listenToTesterMemberships(user?.uid)
            listenToOwnerMemberships(user?.uid)
            listenToDailyTests(user?.uid)
            listenToOwnerDailyTests(user?.uid)
            listenToScoreEvents(user?.uid)
            user?.let(::ensureUserDocument)
        }
        listenToApps()
    }

    override val snapshot: Track14Snapshot
        get() {
            val currentUser = currentUserProfile ?: guestUserProfile()
            return Track14Snapshot(
                currentUser = currentUser,
                rankedApps = apps.sortedByDescending { it.rankingScore },
                joinedTests = testerMemberships.mapNotNull { membership ->
                    val app = apps.firstOrNull { it.id == membership.appId } ?: return@mapNotNull null
                    JoinedTestSnapshot(
                        app = app,
                        membership = membership,
                        dailyTests = buildStreakDailyTests(
                            membership = membership,
                            existingTests = ownerDailyTests
                                .filter { it.membershipId == membership.id }
                                .sortedBy { it.testDate },
                        ),
                    )
                },
                ownerApps = apps.filter { it.ownerId == currentUser.id },
                ownerTesterSnapshots = ownerMemberships.mapNotNull { membership ->
                    val app = apps.firstOrNull { it.id == membership.appId } ?: return@mapNotNull null
                    OwnerTesterSnapshot(
                        appId = app.id,
                        appName = app.name,
                        membershipId = membership.id,
                        testerName = testerLabel(membership.testerId),
                        membershipStatus = membership.status,
                        streak = buildStreakDailyTests(
                            membership = membership,
                            existingTests = dailyTests
                                .filter { it.membershipId == membership.id }
                                .sortedBy { it.testDate },
                        ).map { it.status },
                    )
                },
                scoreEvents = scoreEvents,
            )
        }

    override fun getAppById(appId: String): TrackApp? =
        apps.firstOrNull { it.id == appId }

    override fun addOwnerApp(request: AddOwnerAppRequest): AddOwnerAppResult {
        val owner = currentUserProfile
            ?: return AddOwnerAppResult.Failure("Ilova qo'shish uchun email hisobingiz bilan kiring.")

        val ownerApps = apps.filter { it.ownerId == owner.id }
        if (ownerApps.size >= owner.freeAppLimit) {
            return AddOwnerAppResult.Failure("Bepul limit tugagan: ${owner.freeAppLimit} ta ilova.")
        }

        val packageName = request.packageName.trim()
        if (apps.any { it.packageName.equals(packageName, ignoreCase = true) }) {
            return AddOwnerAppResult.Failure("Bu package name Track14 ro'yxatida bor.")
        }

        val appName = request.name.trim()
        val googleGroupUrl = request.googleGroupUrl.trim()
        val playOptInUrl = request.playOptInUrl.trim()
        val playInstallUrl = request.playInstallUrl.trim()
            .ifBlank { "https://play.google.com/store/apps/details?id=$packageName" }
        val iconUrl = request.iconUrl
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (appName.isBlank() || packageName.isBlank()) {
            return AddOwnerAppResult.Failure("Ilova nomi va package name majburiy.")
        }

        if (!isGoogleGroupUrl(googleGroupUrl)) {
            return AddOwnerAppResult.Failure("Google Group linki http://groups.google... yoki https://groups.google... bilan boshlanishi kerak.")
        }

        if (!playOptInUrl.startsWith("https://play.google.com/apps/testing/", ignoreCase = true)) {
            return AddOwnerAppResult.Failure("Play opt-in linki https://play.google.com/apps/testing/... bo'lishi kerak.")
        }

        val appId = nextAppId(packageName)
        val app = TrackApp(
            id = appId,
            ownerId = owner.id,
            name = appName,
            packageName = packageName,
            googleGroupUrl = googleGroupUrl,
            playOptInUrl = playOptInUrl,
            playInstallUrl = playInstallUrl,
            status = TrackAppStatus.Active,
            testerCount = 0,
            requiredTesterCount = 12,
            rankingScore = ScoringRules.rankingScore(
                ownerScore = owner.ownerScore,
                testerCount = 0,
                requiredTesterCount = 12,
            ),
            ownerScore = owner.ownerScore,
            iconUrl = iconUrl,
        )

        apps = (apps + app).sortedByDescending { it.rankingScore }
        firestore.collection(COLLECTION_APPS)
            .document(appId)
            .set(app.toFirestoreMap(), SetOptions.merge())

        return AddOwnerAppResult.Success(app)
    }

    override fun joinApp(appId: String): JoinAppResult {
        return upsertJoinProgress(
            appId = appId,
            googleGroupOpened = true,
            playOptInOpened = true,
            installOpened = true,
            activate = true,
        )
    }

    override fun markGoogleGroupOpened(appId: String): JoinAppResult =
        upsertJoinProgress(
            appId = appId,
            googleGroupOpened = true,
            playOptInOpened = false,
            installOpened = false,
            activate = false,
        )

    override fun markPlayOptInOpened(appId: String): JoinAppResult =
        upsertJoinProgress(
            appId = appId,
            googleGroupOpened = true,
            playOptInOpened = true,
            installOpened = false,
            activate = false,
        )

    private fun upsertJoinProgress(
        appId: String,
        googleGroupOpened: Boolean,
        playOptInOpened: Boolean,
        installOpened: Boolean,
        activate: Boolean,
    ): JoinAppResult {
        val tester = currentUserProfile
            ?: return JoinAppResult.Failure("Testga qo'shilish uchun email hisobingiz bilan kiring.")
        val app = apps.firstOrNull { it.id == appId }
            ?: return JoinAppResult.Failure("Ilova topilmadi.")

        if (app.ownerId == tester.id) {
            return JoinAppResult.Failure("O'z ilovangizga tester sifatida qo'shila olmaysiz.")
        }

        val existing = testerMemberships.firstOrNull { it.appId == appId && it.testerId == tester.id }
        if (activate && existing?.status == TestMembershipStatus.Active) {
            return JoinAppResult.Failure("Bu testga allaqachon qo'shilgansiz.")
        }

        val membershipId = "membership-$appId-${tester.id}"
        val membership = existing?.copy(
            status = if (activate) TestMembershipStatus.Active else existing.status,
            googleGroupOpened = existing.googleGroupOpened || googleGroupOpened,
            playOptInOpened = existing.playOptInOpened || playOptInOpened,
            installOpened = existing.installOpened || installOpened,
        ) ?: TestMembership(
            id = membershipId,
            appId = appId,
            testerId = tester.id,
            ownerId = app.ownerId,
            status = if (activate) TestMembershipStatus.Active else TestMembershipStatus.Pending,
            joinedDate = DATE_FORMAT.format(Date()),
            completedDaysCount = 0,
            missedDaysCount = 0,
            pointsDelta = 0,
            googleGroupOpened = googleGroupOpened,
            playOptInOpened = playOptInOpened,
            installOpened = installOpened,
        )

        testerMemberships = testerMemberships.filterNot { it.id == membership.id } + membership
        if (activate && existing?.status != TestMembershipStatus.Active) {
            apps = apps.map { existingApp ->
                if (existingApp.id == appId) {
                    existingApp.copy(testerCount = existingApp.testerCount + 1)
                } else {
                    existingApp
                }
            }
        }

        firestore.collection(COLLECTION_MEMBERSHIPS)
            .document(membershipId)
            .set(membership.toFirestoreMap(), SetOptions.merge())
        if (activate && existing?.status != TestMembershipStatus.Active) {
            firestore.collection(COLLECTION_APPS)
                .document(appId)
                .update("testerCount", FieldValue.increment(1))
        }

        return JoinAppResult.Success(membership)
    }

    override fun startDailyTest(membershipId: String): DailyTestResult {
        val membership = testerMemberships.firstOrNull { it.id == membershipId && it.status == TestMembershipStatus.Active }
            ?: return DailyTestResult.Failure("Faol test topilmadi.")
        val today = DATE_FORMAT.format(Date())
        val existing = dailyTests.firstOrNull { it.membershipId == membershipId && it.testDate == today }

        if (existing?.status == DailyTestStatus.Completed) {
            return DailyTestResult.Failure("Bugungi test allaqachon bajarilgan.")
        }
        if (existing?.status == DailyTestStatus.Pending && existing.startedAtMillis != null) {
            return DailyTestResult.Success(existing)
        }

        val now = System.currentTimeMillis()
        val dailyTest = DailyTest(
            id = "daily-$membershipId-$today",
            membershipId = membership.id,
            appId = membership.appId,
            ownerId = membership.ownerId,
            testerId = membership.testerId,
            testDate = today,
            status = DailyTestStatus.Pending,
            startedAtMillis = now,
            eligibleAtMillis = now + DAILY_TEST_MIN_MILLIS,
            completedAtMillis = null,
            elapsedSeconds = null,
            pointsApplied = 0,
        )

        dailyTests = dailyTests.filterNot { it.id == dailyTest.id } + dailyTest
        firestore.collection(COLLECTION_DAILY_TESTS)
            .document(dailyTest.id)
            .set(dailyTest.toFirestoreMap(), SetOptions.merge())

        return DailyTestResult.Success(dailyTest)
    }

    override fun completeEligibleDailyTests() {
        val now = System.currentTimeMillis()
        dailyTests
            .filter { test ->
                test.status == DailyTestStatus.Pending &&
                    test.startedAtMillis != null &&
                    now - test.startedAtMillis >= DAILY_TEST_MIN_MILLIS
            }
            .forEach { test ->
                val elapsedSeconds = ((now - test.startedAtMillis!!) / 1000).toInt()
                val completed = test.copy(
                    status = DailyTestStatus.Completed,
                    completedAtMillis = now,
                    elapsedSeconds = elapsedSeconds,
                    pointsApplied = 2,
                )

                dailyTests = dailyTests.filterNot { it.id == completed.id } + completed
                firestore.collection(COLLECTION_DAILY_TESTS)
                    .document(completed.id)
                    .set(completed.toFirestoreMap(), SetOptions.merge())
                firestore.collection(COLLECTION_MEMBERSHIPS)
                    .document(completed.membershipId)
                    .update(
                        mapOf(
                            "completedDaysCount" to FieldValue.increment(1),
                            "pointsDelta" to FieldValue.increment(2),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
                applyScoreEvent(
                    userId = completed.testerId,
                    sourceType = "dailyTest",
                    sourceId = completed.id,
                    points = ScoringRules.DAILY_TEST_POINTS,
                    reason = "Valid daily test",
                )
            }

        applyMissedDayScoreEvents()
    }

    override fun leaveJoinedTest(membershipId: String): JoinAppResult {
        val membership = testerMemberships.firstOrNull { it.id == membershipId && it.status == TestMembershipStatus.Active }
            ?: return JoinAppResult.Failure("Faol test topilmadi.")

        val left = membership.copy(
            status = TestMembershipStatus.Left,
            pointsDelta = membership.pointsDelta + ScoringRules.LEAVE_TEST_POINTS,
        )
        testerMemberships = testerMemberships.filterNot { it.id == membershipId } + left

        firestore.collection(COLLECTION_MEMBERSHIPS)
            .document(membershipId)
            .set(left.toFirestoreMap(), SetOptions.merge())
        firestore.collection(COLLECTION_APPS)
            .document(membership.appId)
            .update(
                mapOf(
                    "testerCount" to FieldValue.increment(-1),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        applyScoreEvent(
            userId = membership.testerId,
            sourceType = "leaveTest",
            sourceId = membership.id,
            points = ScoringRules.LEAVE_TEST_POINTS,
            reason = "Left joined test before day 14",
        )

        return JoinAppResult.Success(left)
    }

    override fun finishTester(membershipId: String): JoinAppResult =
        updateOwnerTesterStatus(
            membershipId = membershipId,
            status = TestMembershipStatus.Finished,
        )

    override fun rejectTester(membershipId: String): JoinAppResult =
        updateOwnerTesterStatus(
            membershipId = membershipId,
            status = TestMembershipStatus.Rejected,
        )

    override fun updateThemeMode(themeMode: ThemeMode) {
        val user = currentUserProfile ?: return
        currentUserProfile = user.copy(themeMode = themeMode)
        firestore.collection(COLLECTION_USERS)
            .document(user.id)
            .update(
                mapOf(
                    "themeMode" to themeMode.name,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
    }

    private fun updateOwnerTesterStatus(
        membershipId: String,
        status: TestMembershipStatus,
    ): JoinAppResult {
        val ownerId = currentUserProfile?.id
            ?: return JoinAppResult.Failure("Owner action uchun email hisobingiz bilan kiring.")
        val membership = ownerMemberships.firstOrNull { it.id == membershipId }
            ?: return JoinAppResult.Failure("Tester topilmadi.")
        if (membership.ownerId != ownerId) {
            return JoinAppResult.Failure("Bu tester sizning ilovangizga tegishli emas.")
        }

        val updated = membership.copy(status = status)
        ownerMemberships = ownerMemberships.filterNot { it.id == membershipId } + updated
        firestore.collection(COLLECTION_MEMBERSHIPS)
            .document(membershipId)
            .set(updated.toFirestoreMap(), SetOptions.merge())

        return JoinAppResult.Success(updated)
    }

    fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnFailureListener { error ->
                if (error is FirebaseAuthInvalidUserException) {
                    auth.createUserWithEmailAndPassword(email, password)
                }
            }
    }

    fun signOut() {
        auth.signOut()
    }

    private fun listenToApps() {
        appsListener?.remove()
        appsListener = firestore.collection(COLLECTION_APPS)
            .orderBy("rankingScore", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                apps = snapshot.documents.mapNotNull { it.toTrackApp() }
            }
    }

    private fun listenToTesterMemberships(userId: String?) {
        testerMembershipsListener?.remove()
        testerMemberships = emptyList()
        if (userId == null) return

        testerMembershipsListener = firestore.collection(COLLECTION_MEMBERSHIPS)
            .whereEqualTo("testerId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                testerMemberships = snapshot.documents.mapNotNull { it.toTestMembership() }
            }
    }

    private fun listenToOwnerMemberships(userId: String?) {
        ownerMembershipsListener?.remove()
        ownerMemberships = emptyList()
        if (userId == null) return

        ownerMembershipsListener = firestore.collection(COLLECTION_MEMBERSHIPS)
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                ownerMemberships = snapshot.documents.mapNotNull { it.toTestMembership() }
            }
    }

    private fun listenToDailyTests(userId: String?) {
        dailyTestsListener?.remove()
        dailyTests = emptyList()
        if (userId == null) return

        dailyTestsListener = firestore.collection(COLLECTION_DAILY_TESTS)
            .whereEqualTo("testerId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                dailyTests = snapshot.documents.mapNotNull { it.toDailyTest() }
            }
    }

    private fun listenToOwnerDailyTests(userId: String?) {
        ownerDailyTestsListener?.remove()
        ownerDailyTests = emptyList()
        if (userId == null) return

        ownerDailyTestsListener = firestore.collection(COLLECTION_DAILY_TESTS)
            .whereEqualTo("ownerId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                ownerDailyTests = snapshot.documents.mapNotNull { it.toDailyTest() }
            }
    }

    private fun listenToScoreEvents(userId: String?) {
        scoreEventsListener?.remove()
        scoreEvents = emptyList()
        if (userId == null) return

        scoreEventsListener = firestore.collection(COLLECTION_SCORE_EVENTS)
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                scoreEvents = snapshot.documents.mapNotNull { it.toScoreEvent() }
            }
    }

    private fun listenToUser(userId: String?) {
        userListener?.remove()
        if (userId == null) {
            currentUserProfile = null
            return
        }

        userListener = firestore.collection(COLLECTION_USERS)
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                currentUserProfile = snapshot?.toUserProfile() ?: auth.currentUser?.toDefaultUserProfile()
            }
    }

    private fun ensureUserDocument(user: com.google.firebase.auth.FirebaseUser) {
        firestore.collection(COLLECTION_USERS)
            .document(user.uid)
            .set(
                mapOf(
                    "id" to user.uid,
                    "email" to user.email.orEmpty(),
                    "displayName" to displayNameFor(user.email, user.displayName),
                    "ownerScore" to 0,
                    "freeAppLimit" to 3,
                    "themeMode" to ThemeMode.System.name,
                    "timezoneId" to TimeZone.getDefault().id,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
    }

    private fun nextAppId(packageName: String): String {
        val baseId = "app-" + packageName
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        if (apps.none { it.id == baseId }) return baseId

        var suffix = 2
        while (apps.any { it.id == "$baseId-$suffix" }) {
            suffix += 1
        }
        return "$baseId-$suffix"
    }

    private fun isGoogleGroupUrl(value: String): Boolean {
        val normalized = value.trim().lowercase()
        return normalized.startsWith("http://groups.google.") ||
            normalized.startsWith("https://groups.google.")
    }

    private fun applyMissedDayScoreEvents() {
        val todayString = StreakRules.todayString()
        val today = DATE_FORMAT.parse(todayString) ?: return
        testerMemberships
            .filter { it.status == TestMembershipStatus.Active }
            .forEach { membership ->
                buildStreakDailyTests(
                    membership = membership,
                    existingTests = dailyTests.filter { it.membershipId == membership.id },
                )
                    .filter { it.status == DailyTestStatus.Missed }
                    .forEach { missedTest ->
                        val missedDate = DATE_FORMAT.parse(missedTest.testDate) ?: return@forEach
                        if (!missedDate.before(today)) return@forEach

                        val sourceId = "${membership.id}-${missedTest.testDate}"
                        val eventId = scoreEventId(
                            userId = membership.testerId,
                            sourceType = "missedDay",
                            sourceId = sourceId,
                        )
                        if (scoreEvents.any { it.id == eventId }) return@forEach

                        applyScoreEvent(
                            userId = membership.testerId,
                            sourceType = "missedDay",
                            sourceId = sourceId,
                            points = ScoringRules.MISSED_DAY_POINTS,
                            reason = "Missed joined-test day",
                        )
                        firestore.collection(COLLECTION_MEMBERSHIPS)
                            .document(membership.id)
                            .update(
                                mapOf(
                                    "missedDaysCount" to FieldValue.increment(1),
                                    "pointsDelta" to FieldValue.increment(ScoringRules.MISSED_DAY_POINTS.toLong()),
                                    "updatedAt" to FieldValue.serverTimestamp(),
                                ),
                            )
                    }
            }
    }

    private fun applyScoreEvent(
        userId: String,
        sourceType: String,
        sourceId: String,
        points: Int,
        reason: String,
    ) {
        val eventId = scoreEventId(
            userId = userId,
            sourceType = sourceType,
            sourceId = sourceId,
        )
        if (scoreEvents.any { it.id == eventId }) return

        val event = ScoreEvent(
            id = eventId,
            userId = userId,
            sourceType = sourceType,
            sourceId = sourceId,
            points = points,
            reason = reason,
            createdAt = ISO_FORMAT.format(Date()),
        )
        scoreEvents = scoreEvents + event
        val newOwnerScore = ((currentUserProfile?.takeIf { it.id == userId }?.ownerScore ?: 0) + points)
            .coerceAtLeast(0)

        firestore.collection(COLLECTION_SCORE_EVENTS)
            .document(eventId)
            .set(event.toFirestoreMap(), SetOptions.merge())
        firestore.collection(COLLECTION_USERS)
            .document(userId)
            .update(
                mapOf(
                    "ownerScore" to FieldValue.increment(points.toLong()),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )

        if (currentUserProfile?.id == userId) {
            currentUserProfile = currentUserProfile?.copy(ownerScore = newOwnerScore)
            updateOwnerAppsRanking(
                ownerId = userId,
                ownerScore = newOwnerScore,
            )
        }
    }

    private fun updateOwnerAppsRanking(
        ownerId: String,
        ownerScore: Int,
    ) {
        val updatedApps = apps.map { app ->
            if (app.ownerId == ownerId) {
                app.copy(
                    ownerScore = ownerScore,
                    rankingScore = ScoringRules.rankingScore(
                        ownerScore = ownerScore,
                        testerCount = app.testerCount,
                        requiredTesterCount = app.requiredTesterCount,
                    ),
                )
            } else {
                app
            }
        }
        apps = updatedApps.sortedByDescending { it.rankingScore }
        updatedApps
            .filter { it.ownerId == ownerId }
            .forEach { app ->
                firestore.collection(COLLECTION_APPS)
                    .document(app.id)
                    .update(
                        mapOf(
                            "ownerScore" to app.ownerScore,
                            "rankingScore" to app.rankingScore,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                    )
            }
    }

    private fun scoreEventId(
        userId: String,
        sourceType: String,
        sourceId: String,
    ): String {
        val raw = "$userId-$sourceType-$sourceId"
        return "score-" + raw
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    private fun buildStreakDailyTests(
        membership: TestMembership,
        existingTests: List<DailyTest>,
    ): List<DailyTest> {
        return StreakRules.projectFourteenDays(
            membership = membership,
            existingTests = existingTests,
            today = StreakRules.todayString(),
        )
    }

    private fun TrackApp.toFirestoreMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "ownerId" to ownerId,
            "name" to name,
            "packageName" to packageName,
            "googleGroupUrl" to googleGroupUrl,
            "playOptInUrl" to playOptInUrl,
            "playInstallUrl" to playInstallUrl,
            "status" to status.name,
            "testerCount" to testerCount,
            "requiredTesterCount" to requiredTesterCount,
            "rankingScore" to rankingScore,
            "ownerScore" to ownerScore,
            "iconUrl" to iconUrl,
            "updatedAt" to FieldValue.serverTimestamp(),
            "createdAt" to FieldValue.serverTimestamp(),
        )

    private fun TestMembership.toFirestoreMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "appId" to appId,
            "testerId" to testerId,
            "ownerId" to ownerId,
            "status" to status.name,
            "joinedDate" to joinedDate,
            "completedDaysCount" to completedDaysCount,
            "missedDaysCount" to missedDaysCount,
            "pointsDelta" to pointsDelta,
            "googleGroupOpened" to googleGroupOpened,
            "playOptInOpened" to playOptInOpened,
            "installOpened" to installOpened,
            "updatedAt" to FieldValue.serverTimestamp(),
            "joinedAt" to FieldValue.serverTimestamp(),
        )

    private fun DailyTest.toFirestoreMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "membershipId" to membershipId,
            "appId" to appId,
            "ownerId" to ownerId,
            "testerId" to testerId,
            "testDate" to testDate,
            "status" to status.name,
            "startedAtMillis" to startedAtMillis,
            "eligibleAtMillis" to eligibleAtMillis,
            "completedAtMillis" to completedAtMillis,
            "elapsedSeconds" to elapsedSeconds,
            "pointsApplied" to pointsApplied,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

    private fun ScoreEvent.toFirestoreMap(): Map<String, Any?> =
        mapOf(
            "id" to id,
            "userId" to userId,
            "sourceType" to sourceType,
            "sourceId" to sourceId,
            "points" to points,
            "reason" to reason,
            "createdAt" to createdAt,
        )

    private fun DocumentSnapshot.toTrackApp(): TrackApp? {
        val name = getString("name") ?: return null
        val packageName = getString("packageName") ?: return null
        val ownerId = getString("ownerId") ?: return null
        val playInstallUrl = getString("playInstallUrl")
            ?: "https://play.google.com/store/apps/details?id=$packageName"

        return TrackApp(
            id = getString("id") ?: id,
            ownerId = ownerId,
            name = name,
            packageName = packageName,
            googleGroupUrl = getString("googleGroupUrl").orEmpty(),
            playOptInUrl = getString("playOptInUrl").orEmpty(),
            playInstallUrl = playInstallUrl,
            status = enumValueOrDefault(getString("status"), TrackAppStatus.Active),
            testerCount = getLong("testerCount")?.toInt() ?: 0,
            requiredTesterCount = getLong("requiredTesterCount")?.toInt() ?: 12,
            rankingScore = getLong("rankingScore")?.toInt() ?: 0,
            ownerScore = getLong("ownerScore")?.toInt() ?: 0,
            iconUrl = getString("iconUrl"),
        )
    }

    private fun DocumentSnapshot.toTestMembership(): TestMembership? {
        return TestMembership(
            id = getString("id") ?: id,
            appId = getString("appId") ?: return null,
            testerId = getString("testerId") ?: return null,
            ownerId = getString("ownerId") ?: return null,
            status = enumValueOrDefault(getString("status"), TestMembershipStatus.Active),
            joinedDate = getString("joinedDate") ?: getTimestamp("joinedAt")?.toDateString().orEmpty(),
            completedDaysCount = getLong("completedDaysCount")?.toInt() ?: 0,
            missedDaysCount = getLong("missedDaysCount")?.toInt() ?: 0,
            pointsDelta = getLong("pointsDelta")?.toInt() ?: 0,
            googleGroupOpened = getBoolean("googleGroupOpened") ?: false,
            playOptInOpened = getBoolean("playOptInOpened") ?: false,
            installOpened = getBoolean("installOpened") ?: false,
        )
    }

    private fun DocumentSnapshot.toDailyTest(): DailyTest? {
        return DailyTest(
            id = getString("id") ?: id,
            membershipId = getString("membershipId") ?: return null,
            appId = getString("appId") ?: return null,
            ownerId = getString("ownerId").orEmpty(),
            testerId = getString("testerId") ?: return null,
            testDate = getString("testDate") ?: return null,
            status = enumValueOrDefault(getString("status"), DailyTestStatus.Pending),
            startedAtMillis = getLong("startedAtMillis"),
            eligibleAtMillis = getLong("eligibleAtMillis"),
            completedAtMillis = getLong("completedAtMillis"),
            elapsedSeconds = getLong("elapsedSeconds")?.toInt(),
            pointsApplied = getLong("pointsApplied")?.toInt() ?: 0,
        )
    }

    private fun DocumentSnapshot.toScoreEvent(): ScoreEvent? {
        return ScoreEvent(
            id = getString("id") ?: id,
            userId = getString("userId") ?: return null,
            sourceType = getString("sourceType") ?: return null,
            sourceId = getString("sourceId") ?: return null,
            points = getLong("points")?.toInt() ?: 0,
            reason = getString("reason").orEmpty(),
            createdAt = getString("createdAt").orEmpty(),
        )
    }

    private fun DocumentSnapshot.toUserProfile(): UserProfile? {
        val email = getString("email") ?: return null
        return UserProfile(
            id = getString("id") ?: id,
            email = email,
            displayName = getString("displayName") ?: email.substringBefore('@'),
            ownerScore = getLong("ownerScore")?.toInt() ?: 0,
            freeAppLimit = getLong("freeAppLimit")?.toInt() ?: 3,
            themeMode = enumValueOrDefault(getString("themeMode"), ThemeMode.System),
            timezoneId = getString("timezoneId") ?: TimeZone.getDefault().id,
        )
    }

    private fun com.google.firebase.auth.FirebaseUser.toAuthSession(): AuthSession =
        AuthSession(
            userId = uid,
            email = email.orEmpty(),
            displayName = displayNameFor(email, displayName),
        )

    private fun com.google.firebase.auth.FirebaseUser.toDefaultUserProfile(): UserProfile =
        UserProfile(
            id = uid,
            email = email.orEmpty(),
            displayName = displayNameFor(email, displayName),
            ownerScore = 0,
            freeAppLimit = 3,
            themeMode = ThemeMode.System,
            timezoneId = TimeZone.getDefault().id,
        )

    private fun guestUserProfile(): UserProfile =
        UserProfile(
            id = "guest",
            email = "",
            displayName = "Guest",
            ownerScore = 0,
            freeAppLimit = 3,
            themeMode = ThemeMode.System,
            timezoneId = TimeZone.getDefault().id,
        )

    private fun displayNameFor(email: String?, displayName: String?): String =
        displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')?.takeIf { it.isNotBlank() }
            ?: "Track14 user"

    private fun testerLabel(testerId: String): String =
        "Tester " + testerId.takeLast(6).ifBlank { testerId }

    private fun Timestamp.toDateString(): String =
        DATE_FORMAT.format(toDate())

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: default

    private const val COLLECTION_USERS = "users"
    private const val COLLECTION_APPS = "apps"
    private const val COLLECTION_MEMBERSHIPS = "memberships"
    private const val COLLECTION_DAILY_TESTS = "dailyTests"
    private const val COLLECTION_SCORE_EVENTS = "scoreEvents"
    private const val DAILY_TEST_MIN_MILLIS = 30_000L

    private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}
