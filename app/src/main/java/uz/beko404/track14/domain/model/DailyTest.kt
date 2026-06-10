package uz.beko404.track14.domain.model

data class DailyTest(
    val id: String,
    val membershipId: String,
    val appId: String,
    val testerId: String,
    val testDate: String,
    val status: DailyTestStatus,
    val startedAtMillis: Long? = null,
    val eligibleAtMillis: Long? = null,
    val completedAtMillis: Long? = null,
    val elapsedSeconds: Int?,
    val pointsApplied: Int,
)

enum class DailyTestStatus {
    Pending,
    Completed,
    Missed,
}

sealed interface DailyTestResult {
    data class Success(val dailyTest: DailyTest) : DailyTestResult
    data class Failure(val message: String) : DailyTestResult
}
