package uz.beko404.track14.data.apps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.beko404.track14.core.billing.BillingManager
import uz.beko404.track14.data.wallet.transactionFrom
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.TxType

class WalletMapperTest {
    @Test
    fun `maps ledger entries and skips unknown ones`() {
        val tx = transactionFrom("k", mapOf("type" to "earn_day", "amount" to 5L, "refId" to "t1", "createdAt" to 10L))!!
        assertEquals(TxType.EARN_DAY, tx.type)
        assertEquals(Coins(5), tx.amount)
        assertNull(transactionFrom("k", mapOf("type" to "gift", "amount" to 5L)))
        assertNull(transactionFrom("k", mapOf("type" to "bonus")))
    }

    @Test
    fun `account id matches the server`() {
        // Same vector as purchases.test.ts.
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", BillingManager.accountIdOf("abc"))
    }
}
