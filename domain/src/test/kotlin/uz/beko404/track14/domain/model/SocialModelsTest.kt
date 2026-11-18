package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SocialModelsTest {
    @Test
    fun `feedback limits`() {
        assertTrue(FeedbackRules.isValid(NewFeedback(5, setOf(FeedbackTag.BUG), "Ishlayapti")))
        assertFalse(FeedbackRules.isValid(NewFeedback(0, emptySet(), "")))
        assertFalse(FeedbackRules.isValid(NewFeedback(6, emptySet(), "")))
        assertFalse(FeedbackRules.isValid(NewFeedback(3, emptySet(), "x".repeat(1_001))))
    }

    @Test
    fun `report reasons per target`() {
        assertTrue(ReportReason.FAKE_ACTIVITY in ReportReason.forTarget(ReportTarget.TESTER))
        assertFalse(ReportReason.FAKE_ACTIVITY in ReportReason.forTarget(ReportTarget.APP))
        assertEquals(ReportReason.OTHER, ReportReason.forTarget(ReportTarget.APP).last())
    }

    @Test
    fun `wire names match the rules`() {
        val allowed = setOf("spam", "fake", "inappropriate", "not_working", "fraud", "fake_activity", "other")
        assertEquals(allowed, ReportReason.entries.map { it.wireName }.toSet())
        assertEquals(FeedbackTag.IDEA, FeedbackTag.fromWire("idea"))
    }
}
