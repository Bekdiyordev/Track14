package uz.beko404.track14.data.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.beko404.track14.data.wallet.walletBalanceFrom
import uz.beko404.track14.domain.model.Coins

class CatalogMapperTest {

    @Test
    fun `maps a full document`() {
        val app = catalogAppFrom(
            "a1",
            mapOf(
                "name" to "Lug'at Pro",
                "category" to "Ta'lim",
                "joined" to 9L,
                "needed" to 12L,
                "rewardPerDay" to 5L,
                "plan" to "pro",
            ),
        )!!
        assertEquals("a1", app.id)
        assertEquals(9, app.testersJoined)
        assertEquals(Coins(5), app.rewardPerDay)
        assertTrue(app.isPro)
    }

    @Test
    fun `missing optional fields get safe defaults`() {
        val app = catalogAppFrom("a2", mapOf("name" to "Qadam"))!!
        assertEquals(0, app.testersJoined)
        assertEquals(12, app.testersNeeded)
        assertEquals(Coins.ZERO, app.rewardPerDay)
        assertFalse(app.isPro)
    }

    @Test
    fun `document without a name is skipped`() {
        assertNull(catalogAppFrom("a3", mapOf("category" to "Moliya")))
        assertNull(catalogAppFrom("a4", mapOf("name" to "  ")))
    }

    @Test
    fun `wallet balance defaults to zero and accepts numbers`() {
        assertEquals(Coins.ZERO, walletBalanceFrom(null))
        assertEquals(Coins.ZERO, walletBalanceFrom(emptyMap()))
        assertEquals(Coins(1_240), walletBalanceFrom(mapOf("balance" to 1_240L)))
        assertEquals(Coins(-10), walletBalanceFrom(mapOf("balance" to -10)))
    }
}

class CatalogMapperExtraFieldsTest {
    @Test
    fun `reads rating, times and description with limits`() {
        val app = catalogAppFrom(
            "a5",
            mapOf(
                "name" to "Kitobxon",
                "rating" to 4.7,
                "createdAt" to 1_000L,
                "featuredUntil" to 2_000L,
                "shortDesc" to "x".repeat(400),
            ),
        )!!
        assertEquals(4.7, app.rating!!, 0.0001)
        assertEquals(1_000L, app.createdAtMillis)
        assertEquals(2_000L, app.featuredUntilMillis)
        assertEquals(300, app.shortDesc.length)
    }

    @Test
    fun `out of range and negative values are neutralised`() {
        val app = catalogAppFrom(
            "a6",
            mapOf("name" to "X", "rating" to 9, "joined" to -4, "needed" to 0, "rewardPerDay" to -5),
        )!!
        assertNull(app.rating)
        assertEquals(0, app.testersJoined)
        assertEquals(12, app.testersNeeded)
        assertEquals(Coins.ZERO, app.rewardPerDay)
    }
}
