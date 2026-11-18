package uz.beko404.track14.core.billing

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.beko404.track14.domain.model.CoinPacks
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.PurchaseResult
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.WalletRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed interface PurchaseOutcome {
    data class Credited(val coins: Coins) : PurchaseOutcome
    data object Pending : PurchaseOutcome
    data object Cancelled : PurchaseOutcome
    data class Failed(val error: DomainError) : PurchaseOutcome
    data class BillingError(val code: Int) : PurchaseOutcome
}

/**
 * App-wide: every Play purchase event, and any paid-but-unconsumed purchase found after
 * sign-in, is sent to the server. Running outside a screen means leaving the shop
 * mid-purchase never loses coins.
 */
@Singleton
class PurchaseVerifier @Inject constructor(
    private val billing: BillingManager,
    private val wallet: WalletRepository,
    private val auth: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _results = MutableSharedFlow<PurchaseOutcome>(extraBufferCapacity = 8)
    val results: SharedFlow<PurchaseOutcome> = _results.asSharedFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            billing.events.collect { event ->
                when (event) {
                    is BillingEvent.Purchased -> verify(event)
                    BillingEvent.Pending -> _results.emit(PurchaseOutcome.Pending)
                    BillingEvent.Cancelled -> _results.emit(PurchaseOutcome.Cancelled)
                    is BillingEvent.Error -> _results.emit(PurchaseOutcome.BillingError(event.code))
                }
            }
        }
        scope.launch {
            auth.session.filterIsInstance<Session.SignedIn>().distinctUntilChanged().collect { session ->
                billing.unfinishedPurchases(session.uid).forEach { verify(it) }
            }
        }
    }

    private suspend fun verify(purchase: BillingEvent.Purchased) {
        // Wait for a session: the server binds the purchase to the signed-in user.
        auth.session.filterIsInstance<Session.SignedIn>().first()
        var attempt = 0
        while (true) {
            when (val r = wallet.verifyPurchase(purchase.productId, purchase.token)) {
                is DomainResult.Success -> {
                    val outcome = when (r.value) {
                        PurchaseResult.CREDITED -> PurchaseOutcome.Credited(CoinPacks.byId(purchase.productId)?.coins ?: Coins.ZERO)
                        PurchaseResult.PENDING -> PurchaseOutcome.Pending
                    }
                    _results.emit(outcome)
                    return
                }
                is DomainResult.Failure -> {
                    if (r.error == DomainError.Network && attempt < MAX_RETRIES) {
                        attempt++
                        delay(RETRY_DELAY_MS * attempt)
                    } else {
                        _results.emit(PurchaseOutcome.Failed(r.error))
                        return
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 2_000L
    }
}
