package uz.beko404.track14.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Test
import uz.beko404.track14.domain.model.DailyTest
import uz.beko404.track14.domain.model.DailyTestStatus
import uz.beko404.track14.domain.model.TestMembership
import uz.beko404.track14.domain.model.TestMembershipStatus

class StreakRulesTest {
    @Test
    fun projectFourteenDaysPreservesExistingCompletedDays() {
        val membership = membership(joinedDate = "2026-06-01")
        val completed = dailyTest(
            membership = membership,
            testDate = "2026-06-01",
            status = DailyTestStatus.Completed,
        )

        val streak = StreakRules.projectFourteenDays(
            membership = membership,
            existingTests = listOf(completed),
            today = "2026-06-03",
        )

        assertEquals(14, streak.size)
        assertEquals(DailyTestStatus.Completed, streak[0].status)
    }

    @Test
    fun projectFourteenDaysMarksPastMissingDaysAsMissed() {
        val membership = membership(joinedDate = "2026-06-01")

        val streak = StreakRules.projectFourteenDays(
            membership = membership,
            existingTests = emptyList(),
            today = "2026-06-03",
        )

        assertEquals(DailyTestStatus.Missed, streak[0].status)
        assertEquals(DailyTestStatus.Missed, streak[1].status)
        assertEquals(DailyTestStatus.Pending, streak[2].status)
    }

    @Test
    fun projectFourteenDaysLeavesFutureDaysPending() {
        val membership = membership(joinedDate = "2026-06-10")

        val streak = StreakRules.projectFourteenDays(
            membership = membership,
            existingTests = emptyList(),
            today = "2026-06-10",
        )

        assertEquals(14, streak.size)
        assertEquals(List(14) { DailyTestStatus.Pending }, streak.map { it.status })
    }

    private fun membership(joinedDate: String) =
        TestMembership(
            id = "membership-1",
            appId = "app-1",
            testerId = "tester-1",
            ownerId = "owner-1",
            status = TestMembershipStatus.Active,
            joinedDate = joinedDate,
            completedDaysCount = 0,
            missedDaysCount = 0,
            pointsDelta = 0,
        )

    private fun dailyTest(
        membership: TestMembership,
        testDate: String,
        status: DailyTestStatus,
    ) = DailyTest(
        id = "daily-${membership.id}-$testDate",
        membershipId = membership.id,
        appId = membership.appId,
        ownerId = membership.ownerId,
        testerId = membership.testerId,
        testDate = testDate,
        status = status,
        elapsedSeconds = null,
        pointsApplied = 0,
    )
}
