package uz.beko404.track14.domain.rules

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** How a day was verified. Wire names match the server (`usage`, `fallback`). */
enum class CheckMode(val wireName: String) {
    /** Measured from Android usage stats. */
    USAGE_ACCESS("usage"),

    /** No usage permission: time between opening the app from Track14 and coming back. */
    FALLBACK("fallback"),
}

/**
 * A day counts only if the app was in the foreground long enough.
 * The server re-checks the number and computes the day itself; the client is never trusted.
 */
object DailyCheckRules {
    const val MIN_FOREGROUND_SECONDS = 30

    /** Fallback timings above this are not believable for one session. */
    const val MAX_FALLBACK_SECONDS = 3_600

    fun isPassed(foregroundSeconds: Int): Boolean = foregroundSeconds >= MIN_FOREGROUND_SECONDS

    /** Start date is day 1; every local midnight in [zone] starts the next day (same as the server). */
    fun dayNumber(start: Instant, now: Instant, zone: ZoneId): Int =
        ChronoUnit.DAYS.between(start.atZone(zone).toLocalDate(), now.atZone(zone).toLocalDate()).toInt() + 1
}
