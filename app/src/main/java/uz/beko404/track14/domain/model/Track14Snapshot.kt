package uz.beko404.track14.domain.model

data class Track14Snapshot(
    val currentUser: UserProfile,
    val rankedApps: List<TrackApp>,
    val joinedTests: List<JoinedTestSnapshot>,
    val ownerApps: List<TrackApp>,
    val ownerTesterSnapshots: List<OwnerTesterSnapshot>,
    val scoreEvents: List<ScoreEvent>,
)

data class JoinedTestSnapshot(
    val app: TrackApp,
    val membership: TestMembership,
    val dailyTests: List<DailyTest>,
)

data class OwnerTesterSnapshot(
    val testerName: String,
    val membershipStatus: TestMembershipStatus,
    val streak: List<DailyTestStatus>,
)
