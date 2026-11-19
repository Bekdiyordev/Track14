package uz.beko404.track14.domain.model

enum class FeedbackTag(val wireName: String) {
    BUG("bug"),
    IDEA("idea"),
    DESIGN("design"),
    SPEED("speed"),
    POSITIVE("positive");

    companion object {
        fun fromWire(value: String?): FeedbackTag? = entries.firstOrNull { it.wireName == value }
    }
}

data class NewFeedback(val rating: Int, val tags: Set<FeedbackTag>, val text: String)

data class Feedback(
    val id: String,
    val rating: Int,
    val tags: Set<FeedbackTag>,
    val text: String,
    val createdAtMillis: Long,
    val testId: String = "",
)

/** Same limits as firestore.rules (feedback). */
object FeedbackRules {
    const val MAX_TEXT = 1_000
    const val MAX_TAGS = 5

    fun isValid(f: NewFeedback): Boolean =
        f.rating in 1..5 && f.tags.size <= MAX_TAGS && f.text.trim().length <= MAX_TEXT
}

enum class ReportTarget(val wireName: String) { APP("app"), TESTER("tester") }

/** Wire names must match the list in firestore.rules (reports). */
enum class ReportReason(val wireName: String, val targets: Set<ReportTarget>) {
    NOT_WORKING("not_working", setOf(ReportTarget.APP)),
    FAKE("fake", setOf(ReportTarget.APP)),
    INAPPROPRIATE("inappropriate", setOf(ReportTarget.APP)),
    FRAUD("fraud", setOf(ReportTarget.APP)),
    SPAM("spam", setOf(ReportTarget.APP, ReportTarget.TESTER)),
    FAKE_ACTIVITY("fake_activity", setOf(ReportTarget.TESTER)),
    OTHER("other", setOf(ReportTarget.APP, ReportTarget.TESTER));

    companion object {
        fun forTarget(target: ReportTarget) = entries.filter { target in it.targets }
    }
}

data class AppNotification(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val refId: String,
    val read: Boolean,
    val createdAtMillis: Long,
)
