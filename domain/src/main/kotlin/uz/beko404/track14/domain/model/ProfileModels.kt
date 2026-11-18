package uz.beko404.track14.domain.model

/** users/{uid}.stats, written only by Cloud Functions. */
data class TesterStats(
    val testsStarted: Int = 0,
    val testsCompleted: Int = 0,
    val testsFailed: Int = 0,
    val testsLeft: Int = 0,
    val daysPassed: Int = 0,
) {
    /** Same formula as reliability() in stats.ts: completed share of finished tests, 0-100. */
    val reliability: Int?
        get() {
            val done = testsCompleted + testsFailed + testsLeft
            return if (done == 0) null else Math.round(testsCompleted * 100f / done)
        }
}

/** An app that reached production after testing on Track14 (public `published/{appId}`). */
data class PublishedApp(
    val id: String,
    val name: String,
    val category: String,
    val packageName: String,
    val testers: Int,
    val completed: Int,
    val feedbackCount: Int,
    val rating: Double?,
    val publishedAtMillis: Long,
)

data class PublishedTotals(val count: Int = 0, val testers: Int = 0)
