package uz.beko404.track14.admin.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class DashboardMathTest {
    @Test
    fun `revenue uses pack list prices`() {
        assertEquals(1.99 + 6.99 + 6.99, DashboardMath.revenueUsd(listOf(500, 2_000, 2_000)), 0.0001)
        assertEquals(0.0, DashboardMath.revenueUsd(listOf(123)), 0.0)
    }

    @Test
    fun `start of day is local midnight`() {
        val zone = ZoneId.of("Asia/Samarkand")
        val now = ZonedDateTime.of(2026, 11, 19, 20, 15, 0, 0, zone).toInstant().toEpochMilli()
        val midnight = ZonedDateTime.of(2026, 11, 19, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        assertEquals(midnight, DashboardMath.startOfDay(now, zone))
    }

    @Test
    fun `reasons sorted by count`() {
        assertEquals(
            listOf("fake" to 2, "spam" to 2, "other" to 1),
            DashboardMath.reasonCounts(listOf("spam", "other", "fake", "spam", "fake")),
        )
    }

    @Test
    fun `block lengths match the server`() {
        assertEquals(listOf(7, 30, null), DashboardMath.BLOCK_DAYS)
    }
}
