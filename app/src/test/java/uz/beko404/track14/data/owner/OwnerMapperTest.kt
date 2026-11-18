package uz.beko404.track14.data.owner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.beko404.track14.data.economy.economyFrom
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.Plan

class OwnerMapperTest {
    private val app = mapOf("name" to "Lug'at", "packageName" to "uz.a.b", "status" to "active", "category" to "Ta'lim")

    @Test
    fun `app with campaign`() {
        val campaign = mapOf("plan" to "pro", "status" to "paused", "testersNeeded" to 12L, "stats" to mapOf("joined" to 9L, "failed" to 1L))
        val a = ownerAppFrom("a1", app, campaign)!!
        assertEquals(AppStatus.ACTIVE, a.status)
        assertEquals(Plan.PRO, a.campaign!!.plan)
        assertEquals(AppStatus.PAUSED, a.campaign!!.status)
        assertEquals(9, a.campaign!!.joined)
        assertEquals(1, a.campaign!!.failed)
    }

    @Test
    fun `draft without campaign, bad documents skipped`() {
        assertNull(ownerAppFrom("a2", app + ("status" to "draft"), null)!!.campaign)
        assertNull(ownerAppFrom("a3", app + ("status" to "???"), null))
        assertNull(ownerAppFrom("a4", app - "name", null))
    }

    @Test
    fun `economy config keeps defaults for bad values`() {
        val e = economyFrom(mapOf("standardFee" to 900L, "proFee" to -1L, "leftPenalty" to 2.5))
        assertEquals(Coins(900), e.standardFee)
        assertEquals(Coins(1_800), e.proFee)
        assertEquals(Coins(15), e.leftPenalty)
        assertEquals(Coins(1_000), economyFrom(null).standardFee)
    }
}
