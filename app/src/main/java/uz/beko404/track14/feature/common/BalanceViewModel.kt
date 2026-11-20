package uz.beko404.track14.feature.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.WalletRepository
import javax.inject.Inject

/** What the header pill shows: "Kirish" when signed out, the Coin14 balance when signed in. */
sealed interface HeaderBalance {
    data object Unknown : HeaderBalance
    data object SignedOut : HeaderBalance
    data class Balance(val coins: Coins) : HeaderBalance
}

@HiltViewModel
class BalanceViewModel @Inject constructor(auth: AuthRepository, wallet: WalletRepository) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val balance: StateFlow<HeaderBalance> = auth.session.flatMapLatest { session ->
        if (session !is Session.SignedIn) {
            flowOf(HeaderBalance.SignedOut)
        } else {
            wallet.observeBalance(session.uid).map { r ->
                (r as? DomainResult.Success)?.let { HeaderBalance.Balance(it.value) } ?: HeaderBalance.Balance(Coins(0))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeaderBalance.Unknown)
}

/** "1 240": thousands split by a thin space, as on the canvas. */
fun formatCoins(value: Long): String {
    val digits = kotlin.math.abs(value).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (value < 0) "−$digits" else digits
}
