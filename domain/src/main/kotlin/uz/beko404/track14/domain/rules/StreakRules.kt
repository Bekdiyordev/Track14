package uz.beko404.track14.domain.rules

import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.TestFailure
import uz.beko404.track14.domain.model.TestOutcome

/**
 * Decides the state of a test from its day cells.
 *
 * Rules (plan, section 4):
 *  - one missed day is tolerated and the test goes on;
 *  - 2 missed days in a row, or 3 missed days in total, end the test;
 *  - after day 14 the tester needs at least [MIN_PASSED_TO_COMPLETE] green cells.
 *
 * The Cloud Function is the source of truth; this mirror lets the app predict
 * and explain the result without a round trip.
 */
object StreakRules {
    const val TEST_DAYS = 14
    const val MIN_PASSED_TO_COMPLETE = 12
    const val MAX_MISSED_IN_ROW = 1
    const val MAX_MISSED_TOTAL = 2

    fun evaluate(days: List<DayStatus>): TestOutcome {
        require(days.size <= TEST_DAYS) { "A test has at most $TEST_DAYS days, got ${days.size}" }

        var missedTotal = 0
        var missedInRow = 0
        var passed = 0
        var pendingSeen = false

        for (day in days) {
            when (day) {
                DayStatus.PASSED -> {
                    passed++
                    missedInRow = 0
                }
                DayStatus.MISSED -> {
                    missedTotal++
                    missedInRow++
                    if (missedInRow > MAX_MISSED_IN_ROW || missedTotal > MAX_MISSED_TOTAL) {
                        return TestOutcome.Failed(TestFailure.TOO_MANY_MISSED)
                    }
                }
                DayStatus.PENDING -> pendingSeen = true
            }
        }

        val allDaysResolved = days.size == TEST_DAYS && !pendingSeen
        return when {
            !allDaysResolved -> TestOutcome.Active
            passed >= MIN_PASSED_TO_COMPLETE -> TestOutcome.Completed
            else -> TestOutcome.Failed(TestFailure.TOO_MANY_MISSED)
        }
    }
}
