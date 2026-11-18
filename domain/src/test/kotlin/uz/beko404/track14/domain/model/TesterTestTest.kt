package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class TesterTestTest {
    private val zone = "Asia/Samarkand"
    private fun at(day: Int, hour: Int) =
        ZonedDateTime.of(2026, 11, day, hour, 0, 0, 0, ZoneId.of(zone)).toInstant().toEpochMilli()

    private fun test(days: List<DayStatus>, status: TestStatus = TestStatus.ACTIVE, start: Long? = at(16, 20)) =
        TesterTest("t", "c", "a", "Lug'at", "uz.a.b", status, Coins(3), zone, start, days)

    private val pending = List(14) { DayStatus.PENDING }

    @Test
    fun `day number follows local midnight`() {
        val t = test(pending)
        assertEquals(1, t.dayNumber(at(16, 23)))
        assertEquals(2, t.dayNumber(at(17, 0)))
        assertEquals(14, t.dayNumber(at(29, 12)))
        assertNull(test(pending, start = null).dayNumber(at(17, 0)))
    }

    @Test
    fun `today is done only when today's cell is green`() {
        val days = pending.toMutableList().also { it[1] = DayStatus.PASSED }
        val t = test(days)
        assertTrue(t.isDoneToday(at(17, 9)))
        assertFalse(t.needsCheckToday(at(17, 9)))
        assertFalse(t.isDoneToday(at(18, 9)))
        assertTrue(t.needsCheckToday(at(18, 9)))
        assertEquals(1, t.daysPassed)
    }

    @Test
    fun `finished or out of window tests need no check`() {
        assertFalse(test(pending, status = TestStatus.COMPLETED).needsCheckToday(at(17, 9)))
        assertFalse(test(pending).needsCheckToday(at(30, 9)))
    }
}
