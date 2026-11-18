package uz.beko404.track14.domain.model

/** App / campaign life-cycle. Server value = [wireName]. Only Functions change it after "draft". */
enum class AppStatus(val wireName: String) {
    DRAFT("draft"),
    ACTIVE("active"),
    PAUSED("paused"),
    PUBLISHED("published"),
    SUSPENDED("suspended"),
    REMOVED("removed");

    companion object {
        fun fromWire(value: String?): AppStatus? = entries.firstOrNull { it.wireName == value }
    }
}

/** Owner actions on a running campaign (plan: pause / resume / Published). */
enum class OwnerAction(val wireName: String) {
    PAUSE("pause"),
    RESUME("resume"),
    PUBLISH("publish"),
}

data class CampaignSummary(
    val plan: Plan,
    val status: AppStatus,
    val joined: Int,
    val needed: Int,
    val completed: Int,
    val failed: Int,
    val left: Int,
    val startedAtMillis: Long?,
    val featuredUntilMillis: Long = 0L,
)

data class OwnerApp(
    val id: String,
    val name: String,
    val packageName: String,
    val category: String,
    val shortDesc: String,
    val optInUrl: String,
    val status: AppStatus,
    val campaign: CampaignSummary?,
) {
    /** Actions the server will accept now; mirrors `nextStatus` in campaigns.ts. */
    val availableActions: List<OwnerAction>
        get() = when (campaign?.status) {
            AppStatus.ACTIVE -> listOf(OwnerAction.PAUSE, OwnerAction.PUBLISH)
            AppStatus.PAUSED -> listOf(OwnerAction.RESUME, OwnerAction.PUBLISH)
            else -> emptyList()
        }.takeIf { status != AppStatus.SUSPENDED }.orEmpty()
}

/** One tester as the owner sees them: no name or email, only progress. */
data class CampaignTester(
    val testId: String,
    val status: TestStatus,
    val days: List<DayStatus>,
    val startedAtMillis: Long?,
)

/** Fixed categories (stored as shown; translations come with uz/ru/en). */
object AppCategories {
    val all = listOf(
        "Ta'lim", "Moliya", "O'yinlar", "Sog'liq", "Taom", "Turmush tarzi", "Ish va unumdorlik", "Boshqa",
    )
}
