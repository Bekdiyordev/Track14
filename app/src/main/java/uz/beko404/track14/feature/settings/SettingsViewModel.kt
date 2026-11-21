package uz.beko404.track14.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.beko404.track14.core.settings.AppLanguage
import uz.beko404.track14.core.settings.Settings
import uz.beko404.track14.core.settings.SettingsStore
import uz.beko404.track14.core.settings.ThemeMode
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.ProfileRepository
import uz.beko404.track14.domain.repository.Session
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val store: SettingsStore,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {
    val settings: StateFlow<Settings> = store.settings

    val signedIn: StateFlow<Boolean> = auth.session
        .map { it is Session.SignedIn }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _account = MutableStateFlow(AccountState())
    val account: StateFlow<AccountState> = _account.asStateFlow()

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    /** Irreversible: the screen asks for confirmation first. */
    fun deleteAccount() {
        if (_account.value.deleting) return
        _account.value = AccountState(deleting = true)
        viewModelScope.launch {
            _account.value = when (val r = auth.deleteAccount()) {
                is DomainResult.Success -> AccountState(deleted = true)
                is DomainResult.Failure -> AccountState(error = r.error)
            }
        }
    }

    fun clearAccountMessage() {
        _account.value = AccountState()
    }

    val language: AppLanguage get() = store.currentLanguage()

    fun setTheme(mode: ThemeMode) = store.setTheme(mode)

    /** Saved on the device at once and mirrored to the account so the server mutes the push. */
    fun setPush(reminders: Boolean, owner: Boolean) {
        store.setPush(reminders, owner)
        viewModelScope.launch {
            (auth.session.first() as? Session.SignedIn)?.let { profiles.updatePushPrefs(it.uid, reminders, owner) }
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            (auth.session.first() as? Session.SignedIn)?.let { profiles.updateLanguage(it.uid, language.tag) }
            store.setLanguage(language)
        }
    }

    fun previewLanguage(language: AppLanguage) = store.previewLanguage(language)

    /** Language screen: keep the current language but remember that the choice was made. */
    fun confirmLanguage(language: AppLanguage) = setLanguage(language)

    fun completeOnboarding() = store.completeOnboarding()
}

data class AccountState(
    val deleting: Boolean = false,
    val deleted: Boolean = false,
    val error: DomainError? = null,
)
