package uz.beko404.track14.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import uz.beko404.track14.domain.model.AddOwnerAppRequest
import uz.beko404.track14.domain.model.AddOwnerAppResult
import uz.beko404.track14.domain.model.DailyTest
import uz.beko404.track14.domain.model.DailyTestStatus
import uz.beko404.track14.domain.model.JoinedTestSnapshot
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

object FakeTrack14Repository : Track14Repository {
    private val currentUser = UserProfile(
        id = "user-owner-01",
        email = "developer@track14.local",
        displayName = "Bek Developer",
        ownerScore = 48,
        freeAppLimit = 3,
        themeMode = ThemeMode.System,
        timezoneId = "Asia/Samarkand",
    )

    private var apps by mutableStateOf(
        listOf(
        TrackApp(
            id = "app-market-helper",
            ownerId = "user-owner-02",
            name = "Market Helper",
            packageName = "uz.demo.markethelper",
            googleGroupUrl = "https://groups.google.com/g/market-helper-testers",
            playOptInUrl = "https://play.google.com/apps/testing/uz.demo.markethelper",
            playInstallUrl = "https://play.google.com/store/apps/details?id=uz.demo.markethelper",
            status = TrackAppStatus.Active,
            testerCount = 8,
            requiredTesterCount = 12,
            rankingScore = 64,
            ownerScore = 34,
        ),
        TrackApp(
            id = "app-budget-kit",
            ownerId = "user-owner-03",
            name = "Budget Kit",
            packageName = "uz.demo.budgetkit",
            googleGroupUrl = "https://groups.google.com/g/budget-kit-testers",
            playOptInUrl = "https://play.google.com/apps/testing/uz.demo.budgetkit",
            playInstallUrl = "https://play.google.com/store/apps/details?id=uz.demo.budgetkit",
            status = TrackAppStatus.Active,
            testerCount = 11,
            requiredTesterCount = 12,
            rankingScore = 58,
            ownerScore = 41,
        ),
        TrackApp(
            id = "app-track14",
            ownerId = currentUser.id,
            name = "Track14",
            packageName = "uz.beko404.track14",
            googleGroupUrl = TRACK14_GOOGLE_GROUP_URL,
            playOptInUrl = "https://play.google.com/apps/testing/uz.beko404.track14",
            playInstallUrl = "https://play.google.com/store/apps/details?id=uz.beko404.track14",
            status = TrackAppStatus.Active,
            testerCount = 5,
            requiredTesterCount = 12,
            rankingScore = 52,
            ownerScore = currentUser.ownerScore,
        ),
        ),
    )

    private val memberships = listOf(
        TestMembership(
            id = "membership-market-helper",
            appId = "app-market-helper",
            testerId = currentUser.id,
            ownerId = "user-owner-02",
            status = TestMembershipStatus.Active,
            joinedDate = "2026-06-01",
            completedDaysCount = 3,
            missedDaysCount = 1,
            pointsDelta = 5,
        ),
        TestMembership(
            id = "membership-budget-kit",
            appId = "app-budget-kit",
            testerId = currentUser.id,
            ownerId = "user-owner-03",
            status = TestMembershipStatus.Active,
            joinedDate = "2026-06-02",
            completedDaysCount = 2,
            missedDaysCount = 0,
            pointsDelta = 4,
        ),
    )

    private val dailyTestsByMembershipId = mapOf(
        "membership-market-helper" to listOf(
            dailyTest("daily-market-01", "membership-market-helper", "app-market-helper", "2026-06-01", DailyTestStatus.Completed, 42, 2),
            dailyTest("daily-market-02", "membership-market-helper", "app-market-helper", "2026-06-02", DailyTestStatus.Completed, 35, 2),
            dailyTest("daily-market-03", "membership-market-helper", "app-market-helper", "2026-06-03", DailyTestStatus.Missed, null, -1),
            dailyTest("daily-market-04", "membership-market-helper", "app-market-helper", "2026-06-04", DailyTestStatus.Completed, 39, 2),
        ),
        "membership-budget-kit" to listOf(
            dailyTest("daily-budget-01", "membership-budget-kit", "app-budget-kit", "2026-06-02", DailyTestStatus.Completed, 33, 2),
            dailyTest("daily-budget-02", "membership-budget-kit", "app-budget-kit", "2026-06-03", DailyTestStatus.Completed, 31, 2),
            dailyTest("daily-budget-03", "membership-budget-kit", "app-budget-kit", "2026-06-04", DailyTestStatus.Pending, null, 0),
        ),
    )

    private val ownerTesterSnapshots = listOf(
        OwnerTesterSnapshot(
            testerName = "Ali Tester",
            membershipStatus = TestMembershipStatus.Active,
            streak = listOf(
                DailyTestStatus.Completed,
                DailyTestStatus.Completed,
                DailyTestStatus.Completed,
                DailyTestStatus.Pending,
            ),
        ),
        OwnerTesterSnapshot(
            testerName = "Madina Dev",
            membershipStatus = TestMembershipStatus.Active,
            streak = listOf(
                DailyTestStatus.Completed,
                DailyTestStatus.Missed,
                DailyTestStatus.Completed,
                DailyTestStatus.Pending,
            ),
        ),
    )

    override val snapshot: Track14Snapshot
        get() = Track14Snapshot(
            currentUser = currentUser,
            rankedApps = apps.sortedByDescending { it.rankingScore },
            joinedTests = memberships.mapNotNull { membership ->
                val app = apps.firstOrNull { it.id == membership.appId } ?: return@mapNotNull null
                JoinedTestSnapshot(
                    app = app,
                    membership = membership,
                    dailyTests = dailyTestsByMembershipId[membership.id].orEmpty(),
                )
            },
            ownerApps = apps.filter { it.ownerId == currentUser.id },
            ownerTesterSnapshots = ownerTesterSnapshots,
            scoreEvents = listOf(
                ScoreEvent(
                    id = "score-01",
                    userId = currentUser.id,
                    sourceType = "dailyTest",
                    sourceId = "daily-market-01",
                    points = 2,
                    reason = "Valid daily test",
                    createdAt = "2026-06-01T09:00:00Z",
                ),
            ),
        )

    override fun getAppById(appId: String): TrackApp? =
        apps.firstOrNull { it.id == appId }

    override fun addOwnerApp(request: AddOwnerAppRequest): AddOwnerAppResult {
        val ownerApps = apps.filter { it.ownerId == currentUser.id }
        if (ownerApps.size >= currentUser.freeAppLimit) {
            return AddOwnerAppResult.Failure("Bepul limit tugagan: ${currentUser.freeAppLimit} ta ilova.")
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

        val newApp = TrackApp(
            id = nextAppId(packageName),
            ownerId = currentUser.id,
            name = appName,
            packageName = packageName,
            googleGroupUrl = googleGroupUrl,
            playOptInUrl = playOptInUrl,
            playInstallUrl = playInstallUrl,
            status = TrackAppStatus.Active,
            testerCount = 0,
            requiredTesterCount = 12,
            rankingScore = currentUser.ownerScore + 12,
            ownerScore = currentUser.ownerScore,
            iconUrl = iconUrl,
        )

        apps = apps + newApp
        return AddOwnerAppResult.Success(newApp)
    }

    private fun nextAppId(packageName: String): String {
        val baseId = "app-" + packageName
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')

        if (apps.none { it.id == baseId }) {
            return baseId
        }

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

    private fun dailyTest(
        id: String,
        membershipId: String,
        appId: String,
        testDate: String,
        status: DailyTestStatus,
        elapsedSeconds: Int?,
        pointsApplied: Int,
    ) = DailyTest(
        id = id,
        membershipId = membershipId,
        appId = appId,
        testerId = currentUser.id,
        testDate = testDate,
        status = status,
        elapsedSeconds = elapsedSeconds,
        pointsApplied = pointsApplied,
    )

    private const val TRACK14_GOOGLE_GROUP_URL = "http://groups.google.com/g/track14-testers"
}
