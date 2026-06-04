package uz.beko404.track14.domain.model

data class DailyTest(
    val id: String,
    val membershipId: String,
    val appId: String,
    val testerId: String,
    val testDate: String,
    val status: DailyTestStatus,
    val elapsedSeconds: Int?,
    val pointsApplied: Int,
)

enum class DailyTestStatus {
    Pending,
    Completed,
    Missed,
}
