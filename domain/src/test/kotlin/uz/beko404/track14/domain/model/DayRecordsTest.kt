package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.beko404.track14.domain.rules.CheckMode
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DayRecordsTest {
    private val zone = "Asia/Samarkand"
    private val start = ZonedDateTime.of(2026, 11, 9, 21, 30, 0, 0, ZoneId.of(zone)).toInstant().toEpochMilli()
    private fun days(pattern: String) = pattern.map {
        when (it) { 'G' -> DayStatus.PASSED; 'R' -> DayStatus.MISSED; else -> DayStatus.PENDING }
    }

    @Test
    fun `history keeps closed days newest first`() {
        val records = listOf(
            DayRecord(1, DayStatus.PASSED, 34, CheckMode.USAGE_ACCESS),
            DayRecord(3, DayStatus.PENDING),
            DayRecord(2, DayStatus.MISSED),
            DayRecord(2, DayStatus.MISSED),
            DayRecord(20, DayStatus.PASSED),
        )
        assertEquals(listOf(2, 1), DayHistory.closed(records).map { it.day })
    }

    @Test
    fun `dates follow the tester zone`() {
        assertEquals(LocalDate.of(2026, 11, 9), DayHistory.dateOf(start, 1, zone))
        assertEquals(LocalDate.of(2026, 11, 14), DayHistory.dateOf(start, 6, zone))
        assertNull(DayHistory.dateOf(null, 1, zone))
    }

    @Test
    fun `warning counts missed days of an active test`() {
        assertNull(DayHistory.warning(days("GGGG.........."), TestStatus.ACTIVE))
        assertEquals(MissedWarning(1, 3), DayHistory.warning(days("GGGGRG........"), TestStatus.ACTIVE))
        assertNull(DayHistory.warning(days("GGGGRG........"), TestStatus.COMPLETED))
    }

    @Test
    fun `earned and days left`() {
        val t = TesterTest("t", "c", "a", "App", "uz.a.b", TestStatus.ACTIVE, Coins(5), zone, start, days("GGGGRG........"))
        assertEquals(Coins(25), DayHistory.earned(t))
        assertEquals(8, DayHistory.daysLeft(6))
        assertEquals(0, DayHistory.daysLeft(20))
        assertEquals(14, DayHistory.daysLeft(null))
    }

    @Test
    fun `fallback mode is noticed`() {
        assertTrue(DayHistory.usedFallback(listOf(DayRecord(1, DayStatus.PASSED, 41, CheckMode.FALLBACK))))
        assertFalse(DayHistory.usedFallback(listOf(DayRecord(1, DayStatus.PASSED, 41, CheckMode.USAGE_ACCESS))))
    }

    @Test
    fun `initials for the avatar`() {
        assertEquals("SK", CampaignTester("t", TestStatus.ACTIVE, days(".............."), null, name = "Sardor K.").initials)
        assertEquals("?", CampaignTester("t", TestStatus.ACTIVE, days(".............."), null).initials)
    }
}
