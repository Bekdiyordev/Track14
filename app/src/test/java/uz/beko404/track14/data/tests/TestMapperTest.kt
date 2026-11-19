package uz.beko404.track14.data.tests

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.rules.CheckMode

class TestMapperTest {
    private val base = mapOf(
        "campaignId" to "c1",
        "appId" to "c1",
        "appName" to "Lug'at Pro",
        "packageName" to "uz.app.lugat",
        "status" to "active",
        "rewardPerDay" to 5L,
        "timezone" to "Asia/Samarkand",
        "startedAt" to 1_000L,
    )

    @Test
    fun `maps a test with its days`() {
        val t = testerTestFrom("c1_u1", base, mapOf(1 to DayStatus.PASSED, 2 to DayStatus.MISSED))!!
        assertEquals(TestStatus.ACTIVE, t.status)
        assertEquals(Coins(5), t.rewardPerDay)
        assertEquals(14, t.days.size)
        assertEquals(DayStatus.PASSED, t.days[0])
        assertEquals(DayStatus.MISSED, t.days[1])
        assertEquals(DayStatus.PENDING, t.days[13])
        assertEquals(1_000L, t.startedAtMillis)
    }

    @Test
    fun `bad package and unknown status are handled`() {
        assertEquals("", testerTestFrom("x", base + ("packageName" to "x;y"), emptyMap())!!.packageName)
        assertNull(testerTestFrom("x", base + ("status" to "hacked"), emptyMap()))
        assertNull(testerTestFrom("x", base - "campaignId", emptyMap()))
    }

    @Test
    fun `day status wire names`() {
        assertEquals(DayStatus.PASSED, dayStatusFrom("passed"))
        assertEquals(DayStatus.MISSED, dayStatusFrom("missed"))
        assertEquals(DayStatus.PENDING, dayStatusFrom(null))
    }

    @Test
    fun `maps day records and drops bad ids`() {
        val r = dayRecordFrom("3", mapOf("status" to "passed", "seconds" to 47L, "mode" to "fallback"))!!
        assertEquals(3, r.day)
        assertEquals(DayStatus.PASSED, r.status)
        assertEquals(47, r.seconds)
        assertEquals(CheckMode.FALLBACK, r.mode)
        assertNull(dayRecordFrom("15", mapOf("status" to "passed")))
        assertNull(dayRecordFrom("x", mapOf("status" to "passed")))
        assertNull(dayRecordFrom("2", mapOf("status" to "missed"))!!.mode)
    }
}
