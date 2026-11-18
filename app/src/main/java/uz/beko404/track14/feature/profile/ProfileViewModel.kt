package uz.beko404.track14.feature.profile

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
import uz.beko404.track14.domain.model.TesterStats
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.ProfileRepository
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.WalletRepository
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    auth: AuthRepository,
    wallet: WalletRepository,
    profiles: ProfileRepository,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<TesterStats> = auth.session
        .flatMapLatest { s -> if (s is Session.SignedIn) profiles.observeStats(s.uid) else flowOf(TesterStats()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TesterStats())


    /** Null while signed out or until the first wallet snapshot arrives. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val balance: StateFlow<Coins?> = auth.session
        .flatMapLatest { session ->
            when (session) {
                is Session.SignedIn -> wallet.observeBalance(session.uid)
                    .map { (it as? DomainResult.Success)?.value }
                Session.SignedOut -> flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
