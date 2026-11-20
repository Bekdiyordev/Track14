package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CampaignTesterTest {
    private val day = 86_400_000L
    private val start = 1_700_000_000_000L - (1_700_000_000_000L % day) // UTC midnight

    private fun tester(vararg d: DayStatus) =
        CampaignTester("t1", TestStatus.ACTIVE, d.toList() + List(14 - d.size) { DayStatus.PENDING }, start)

    @Test
    fun `day number counts calendar days in the tester zone`() {
        val t = tester()
        assertEquals(1, t.dayNumber(start + 1_000))
        assertEquals(3, t.dayNumber(start + 2 * day + 5_000))
        assertNull(t.copy(startedAtMillis = null).dayNumber(start))
    }

    @Test
    fun `streak counts passed days in a row, today included only when passed`() {
        val p = DayStatus.PASSED
        val m = DayStatus.MISSED
        // day 4, today still pending: streak is days 1..3
        assertEquals(3, tester(p, p, p).streak(start + 3 * day + 10))
        // day 4 and today passed
        assertEquals(4, tester(p, p, p, p).streak(start + 3 * day + 10))
        // a miss on day 2 resets
        assertEquals(1, tester(p, m, p).streak(start + 3 * day + 10))
        assertEquals(0, tester().streak(start + 10))
    }
}
