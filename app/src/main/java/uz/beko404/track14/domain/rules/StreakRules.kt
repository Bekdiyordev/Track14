package uz.beko404.track14.domain.rules

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import uz.beko404.track14.domain.model.DailyTest
import uz.beko404.track14.domain.model.DailyTestStatus
import uz.beko404.track14.domain.model.TestMembership

object StreakRules {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun projectFourteenDays(
        membership: TestMembership,
        existingTests: List<DailyTest>,
        today: String,
    ): List<DailyTest> {
        val joinedDate = dateFormat.parse(membership.joinedDate) ?: return existingTests
        val todayDate = dateFormat.parse(today) ?: return existingTests
        val existingByDate = existingTests.associateBy { it.testDate }
        val calendar = Calendar.getInstance().apply { time = joinedDate }

        return List(14) {
            val date = dateFormat.format(calendar.time)
            val existing = existingByDate[date]
            val status = when {
                existing != null -> existing.status
                calendar.time.before(todayDate) -> DailyTestStatus.Missed
                else -> DailyTestStatus.Pending
            }
            val test = existing ?: DailyTest(
                id = "virtual-${membership.id}-$date",
                membershipId = membership.id,
                appId = membership.appId,
                ownerId = membership.ownerId,
                testerId = membership.testerId,
                testDate = date,
                status = status,
                elapsedSeconds = null,
                pointsApplied = 0,
            )
            calendar.add(Calendar.DATE, 1)
            test
        }
    }

    fun todayString(date: Date = Date()): String = dateFormat.format(date)
}
