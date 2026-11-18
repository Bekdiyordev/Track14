package uz.beko404.track14.feature.wallet

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.beko404.track14.core.billing.BillingManager
import uz.beko404.track14.core.billing.PackOffer
import uz.beko404.track14.core.billing.PurchaseOutcome
import uz.beko404.track14.core.billing.PurchaseVerifier
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.TxFilter
import uz.beko404.track14.domain.model.WalletTransaction
import uz.beko404.track14.domain.model.filterBy
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.ConfigRepository
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.WalletRepository
import javax.inject.Inject

data class WalletUiState(
    val signedIn: Boolean = true,
    val balance: Coins? = null,
    val all: List<WalletTransaction> = emptyList(),
    val filter: TxFilter = TxFilter.ALL,
    /** config/flags.purchases: the admin can close the shop. */
    val purchasesEnabled: Boolean = true,
) {
    val shown: List<WalletTransaction> get() = all.filterBy(filter)
    val earned: Coins get() = Coins(all.filter { it.isIncome }.sumOf { it.amount.value })
    val spent: Coins get() = Coins(-all.filter { !it.isIncome }.sumOf { it.amount.value })
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WalletViewModel @Inject constructor(
    auth: AuthRepository,
    wallet: WalletRepository,
    config: ConfigRepository,
) : ViewModel() {
    private val filter = MutableStateFlow(TxFilter.ALL)

    val state: StateFlow<WalletUiState> = auth.session.flatMapLatest { s ->
        if (s !is Session.SignedIn) {
            flowOf(WalletUiState(signedIn = false))
        } else {
            combine(wallet.observeBalance(s.uid), wallet.observeTransactions(s.uid), filter, config.flags()) { b, t, f, flags ->
                WalletUiState(
                    balance = (b as? DomainResult.Success)?.value,
                    all = (t as? DomainResult.Success)?.value.orEmpty(),
                    filter = f,
                    purchasesEnabled = flags.purchases,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WalletUiState())

    fun setFilter(f: TxFilter) {
        filter.value = f
    }
}

data class ShopUiState(
    val loading: Boolean = true,
    val offers: List<PackOffer> = emptyList(),
    val selected: String? = null,
    val balance: Coins? = null,
    val launching: Boolean = false,
    val outcome: PurchaseOutcome? = null,
) {
    val selectedOffer: PackOffer? get() = offers.firstOrNull { it.pack.productId == selected }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ShopViewModel @Inject constructor(
    private val auth: AuthRepository,
    wallet: WalletRepository,
    private val billing: BillingManager,
    verifier: PurchaseVerifier,
) : ViewModel() {
    private val local = MutableStateFlow(ShopUiState())

    private val balance = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn) wallet.observeBalance(s.uid).map { (it as? DomainResult.Success)?.value } else flowOf(null)
    }

    val state: StateFlow<ShopUiState> = combine(local, balance) { l, b -> l.copy(balance = b) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShopUiState())

    init {
        viewModelScope.launch {
            val offers = billing.loadOffers()
            local.update { s ->
                s.copy(
                    loading = false,
                    offers = offers,
                    selected = offers.firstOrNull { it.pack.popular }?.pack?.productId ?: offers.firstOrNull()?.pack?.productId,
                )
            }
        }
        viewModelScope.launch {
            verifier.results.collect { outcome -> local.update { it.copy(launching = false, outcome = outcome) } }
        }
    }

    fun select(productId: String) = local.update { it.copy(selected = productId, outcome = null) }

    fun dismissOutcome() = local.update { it.copy(outcome = null) }

    /** Activity is passed per call and never stored. */
    fun buy(activity: Activity) {
        val offer = state.value.selectedOffer ?: return
        if (state.value.launching) return
        viewModelScope.launch {
            val session = auth.session.first() as? Session.SignedIn ?: return@launch
            local.update { it.copy(launching = true, outcome = null) }
            if (!billing.launch(activity, offer, session.uid)) {
                local.update { it.copy(launching = false, outcome = PurchaseOutcome.BillingError(-1)) }
            }
        }
    }
}
