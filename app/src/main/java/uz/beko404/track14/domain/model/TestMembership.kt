package uz.beko404.track14.domain.model

data class TestMembership(
    val id: String,
    val appId: String,
    val testerId: String,
    val ownerId: String,
    val status: TestMembershipStatus,
    val joinedDate: String,
    val completedDaysCount: Int,
    val missedDaysCount: Int,
    val pointsDelta: Int,
)

enum class TestMembershipStatus {
    Active,
    Finished,
    Rejected,
    Left,
}
