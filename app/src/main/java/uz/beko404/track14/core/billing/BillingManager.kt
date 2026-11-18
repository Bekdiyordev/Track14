package uz.beko404.track14.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.beko404.track14.domain.model.CoinPack
import uz.beko404.track14.domain.model.CoinPacks
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class PackOffer(val pack: CoinPack, val price: String, internal val details: ProductDetails)

sealed interface BillingEvent {
    /** Paid; must be verified by the server before any coins appear. */
    data class Purchased(val productId: String, val token: String) : BillingEvent
    data object Pending : BillingEvent
    data object Cancelled : BillingEvent
    data class Error(val code: Int) : BillingEvent
}

/**
 * Thin wrapper around Play Billing. It never grants coins itself: every purchase goes to
 * `verifyPurchase`, which checks it with Google, credits it once and consumes it.
 */
@Singleton
class BillingManager @Inject constructor(@ApplicationContext context: Context) {

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private val listener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach { emit(it) }
            BillingClient.BillingResponseCode.USER_CANCELED -> _events.tryEmit(BillingEvent.Cancelled)
            else -> _events.tryEmit(BillingEvent.Error(result.responseCode))
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(listener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private val connectLock = Mutex()

    private suspend fun connect(): Boolean = connectLock.withLock {
        if (client.isReady) return@withLock true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    /** Packs that exist in Play Console, with the localised price. Empty when Play is unavailable. */
    suspend fun loadOffers(): List<PackOffer> {
        if (!connect()) return emptyList()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                CoinPacks.all.map {
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(it.productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                },
            )
            .build()
        val details: List<ProductDetails> = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { _, result ->
                if (cont.isActive) cont.resume(result.productDetailsList)
            }
        }
        return CoinPacks.all.mapNotNull { pack ->
            val d = details.firstOrNull { it.productId == pack.productId } ?: return@mapNotNull null
            @Suppress("DEPRECATION")
            val price = d.oneTimePurchaseOfferDetails?.formattedPrice ?: return@mapNotNull null
            PackOffer(pack, price, d)
        }
    }

    fun launch(activity: Activity, offer: PackOffer, uid: String): Boolean {
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(offer.details).build()),
            )
            .setObfuscatedAccountId(accountIdOf(uid))
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Paid but not yet consumed purchases of [uid] (for example the app was killed mid-purchase). */
    suspend fun unfinishedPurchases(uid: String): List<BillingEvent.Purchased> {
        if (!connect()) return emptyList()
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val purchases: List<Purchase> = suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { _, list -> if (cont.isActive) cont.resume(list) }
        }
        val account = accountIdOf(uid)
        return purchases
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && it.accountIdentifiers?.obfuscatedAccountId == account }
            .flatMap { p -> p.products.map { BillingEvent.Purchased(it, p.purchaseToken) } }
    }

    private fun emit(purchase: Purchase) {
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> purchase.products.forEach {
                _events.tryEmit(BillingEvent.Purchased(it, purchase.purchaseToken))
            }
            Purchase.PurchaseState.PENDING -> _events.tryEmit(BillingEvent.Pending)
            else -> Unit
        }
    }

    companion object {
        /** Must equal accountIdOf in purchases.ts: sha256(uid) as lowercase hex. */
        fun accountIdOf(uid: String): String =
            MessageDigest.getInstance("SHA-256").digest(uid.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
