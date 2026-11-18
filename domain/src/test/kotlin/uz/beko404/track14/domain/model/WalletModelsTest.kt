package uz.beko404.track14.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WalletModelsTest {
    private val txs = listOf(
        WalletTransaction("a", TxType.EARN_DAY, Coins(5), "t", 3),
        WalletTransaction("b", TxType.LISTING_FEE, Coins(-1_000), "app", 2),
        WalletTransaction("c", TxType.PURCHASE, Coins(2_000), "GPA.1", 1),
    )

    @Test
    fun `filters income and spending`() {
        assertEquals(listOf("a", "c"), txs.filterBy(TxFilter.INCOME).map { it.id })
        assertEquals(listOf("b"), txs.filterBy(TxFilter.SPENT).map { it.id })
        assertEquals(3, txs.filterBy(TxFilter.ALL).size)
    }

    @Test
    fun `packs and wire names`() {
        assertEquals(Coins(2_000), CoinPacks.byId("coins_2000")!!.coins)
        assertNull(CoinPacks.byId("coins_free"))
        assertEquals(1, CoinPacks.all.count { it.popular })
        assertEquals(TxType.LISTING_FEE, TxType.fromWire("listing_fee"))
        assertNull(TxType.fromWire("hack"))
    }
}
