package uz.beko404.track14.domain.model

import uz.beko404.track14.domain.rules.DailyCheckRules
import uz.beko404.track14.domain.rules.StreakRules
import java.time.Instant
import java.time.ZoneId

/** One test as the tester sees it. [days] always has 14 entries, index 0 = day 1. */
data class TesterTest(
    val id: String,
    val campaignId: String,
    val appId: String,
    val appName: String,
    val packageName: String,
    val status: TestStatus,
    val rewardPerDay: Coins,
    val timezone: String,
    val startedAtMillis: Long?,
    val days: List<DayStatus>,
    val ownerUid: String = "",
    val feedbackRewarded: Boolean = false,
    val completionBonus: Coins = Coins(0),
) {
    init {
        require(days.size == StreakRules.TEST_DAYS) { "days must have ${StreakRules.TEST_DAYS} entries" }
    }

    val daysPassed: Int get() = days.count { it == DayStatus.PASSED }

    /** Test day (1..14) for [nowMillis], or null before the server has set the start time. */
    fun dayNumber(nowMillis: Long): Int? {
        val start = startedAtMillis ?: return null
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        return DailyCheckRules.dayNumber(Instant.ofEpochMilli(start), Instant.ofEpochMilli(nowMillis), zone)
    }

    fun isDoneToday(nowMillis: Long): Boolean {
        val day = dayNumber(nowMillis) ?: return false
        return day in 1..StreakRules.TEST_DAYS && days[day - 1] == DayStatus.PASSED
    }

    /** Active and today's check is still open. */
    fun needsCheckToday(nowMillis: Long): Boolean {
        val day = dayNumber(nowMillis) ?: return false
        return status == TestStatus.ACTIVE && day in 1..StreakRules.TEST_DAYS && !isDoneToday(nowMillis)
    }
}
