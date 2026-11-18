package uz.beko404.track14.domain.model

/** One of the 14 calendar days of a test. */
enum class DayStatus { PENDING, PASSED, MISSED }

/** Life-cycle of one tester's test of one app. */
enum class TestStatus { JOINING, ACTIVE, COMPLETED, FAILED, LEFT }

/** Why a test ended badly; decides the penalty. */
enum class TestFailure {
    /** Too many missed days (see [uz.beko404.track14.domain.rules.StreakRules]). */
    TOO_MANY_MISSED,

    /** Tester uninstalled the app or left the test. */
    LEFT_TEST,
}

/** Result of evaluating the 14 day cells. */
sealed interface TestOutcome {
    /** Test is still going. */
    data object Active : TestOutcome

    /** 14 days are over and the tester qualified. */
    data object Completed : TestOutcome

    data class Failed(val reason: TestFailure) : TestOutcome
}
