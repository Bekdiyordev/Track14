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
    val googleGroupOpened: Boolean = false,
    val playOptInOpened: Boolean = false,
    val installOpened: Boolean = false,
)

enum class TestMembershipStatus {
    Pending,
    Active,
    Finished,
    Rejected,
    Left,
}

sealed interface JoinAppResult {
    data class Success(val membership: TestMembership) : JoinAppResult
    data class Failure(val message: String) : JoinAppResult
}
