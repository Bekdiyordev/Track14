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
    owner: uz.beko404.track14.domain.repository.OwnerRepository,
    notifications: uz.beko404.track14.domain.repository.NotificationsRepository,
    config: uz.beko404.track14.domain.repository.ConfigRepository,
) : ViewModel() {

    /** Profile counters beside TesterStats: coins earned in total, own apps, unread messages. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val extras: StateFlow<ProfileExtras> = auth.session
        .flatMapLatest { s ->
            if (s !is Session.SignedIn) {
                flowOf(ProfileExtras())
            } else {
                kotlinx.coroutines.flow.combine(
                    wallet.observeTransactions(s.uid),
                    owner.observeMyApps(s.uid),
                    notifications.observe(s.uid),
                ) { tx, apps, inbox ->
                    ProfileExtras(
                        earnedTotal = (tx as? DomainResult.Success)?.value.orEmpty()
                            .filter { it.type in EARNING_TYPES && it.amount.value > 0 }.sumOf { it.amount.value },
                        appsCount = (apps as? DomainResult.Success)?.value?.size ?: 0,
                        unread = (inbox as? DomainResult.Success)?.value?.count { !it.read } ?: 0,
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileExtras())

    val telegram: StateFlow<String?> = config.support().map { it.safeTelegram }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

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

data class ProfileExtras(val earnedTotal: Long = 0, val appsCount: Int = 0, val unread: Int = 0)

/** Coins the user earned by testing (not bought, not refunds or admin corrections). */
private val EARNING_TYPES = setOf(
    uz.beko404.track14.domain.model.TxType.EARN_DAY,
    uz.beko404.track14.domain.model.TxType.BONUS,
    uz.beko404.track14.domain.model.TxType.FEEDBACK,
    uz.beko404.track14.domain.model.TxType.WELCOME,
)
