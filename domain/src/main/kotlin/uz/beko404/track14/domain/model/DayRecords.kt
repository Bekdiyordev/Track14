package uz.beko404.track14.domain.model

import uz.beko404.track14.domain.rules.CheckMode
import uz.beko404.track14.domain.rules.StreakRules
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One `tests/{id}/days/{n}` document: what the server recorded for that day. */
data class DayRecord(
    val day: Int,
    val status: DayStatus,
    val seconds: Int? = null,
    val mode: CheckMode? = null,
    val verifiedAtMillis: Long? = null,
)

/** How close an active test is to failing on missed days (shown as "1 / 3"). */
data class MissedWarning(val missed: Int, val limit: Int)

object DayHistory {
    /** A test fails on the missed day after the allowed total. */
    const val MISSED_LIMIT = StreakRules.MAX_MISSED_TOTAL + 1

    /** Closed days, newest first; pending days have nothing to show yet. */
    fun closed(records: List<DayRecord>): List<DayRecord> =
        records.filter { it.status != DayStatus.PENDING && it.day in 1..StreakRules.TEST_DAYS }
            .distinctBy { it.day }
            .sortedByDescending { it.day }

    /** Calendar date of test day [day] in the tester's zone, or null before the start time is known. */
    fun dateOf(startedAtMillis: Long?, day: Int, timezone: String): LocalDate? {
        val start = startedAtMillis ?: return null
        val zone = runCatching { ZoneId.of(timezone) }.getOrDefault(ZoneId.systemDefault())
        return Instant.ofEpochMilli(start).atZone(zone).toLocalDate().plusDays((day - 1).toLong())
    }

    /** Null while nothing was missed, or when the test is no longer running. */
    fun warning(days: List<DayStatus>, status: TestStatus): MissedWarning? {
        if (status != TestStatus.ACTIVE) return null
        val missed = days.count { it == DayStatus.MISSED }
        return if (missed == 0) null else MissedWarning(missed, MISSED_LIMIT)
    }

    /** Days still to go after [today] (0 once the window is over). */
    fun daysLeft(today: Int?): Int = if (today == null) StreakRules.TEST_DAYS else (StreakRules.TEST_DAYS - today).coerceAtLeast(0)

    /** Coins earned so far: passed days times the daily reward. */
    fun earned(test: TesterTest): Coins = Coins(test.daysPassed * test.rewardPerDay.value)

    /** True when the tester used the less precise mode B on any recorded day. */
    fun usedFallback(records: List<DayRecord>): Boolean = records.any { it.mode == CheckMode.FALLBACK }
}
