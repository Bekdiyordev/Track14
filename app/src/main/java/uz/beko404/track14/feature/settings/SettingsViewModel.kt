package uz.beko404.track14.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uz.beko404.track14.core.settings.AppLanguage
import uz.beko404.track14.core.settings.Settings
import uz.beko404.track14.core.settings.SettingsStore
import uz.beko404.track14.core.settings.ThemeMode
import uz.beko404.track14.domain.repository.AuthRepository
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

    val language: AppLanguage get() = store.currentLanguage()

    fun setTheme(mode: ThemeMode) = store.setTheme(mode)

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            (auth.session.first() as? Session.SignedIn)?.let { profiles.updateLanguage(it.uid, language.tag) }
            store.setLanguage(language)
        }
    }

    /** Language screen: keep the current language but remember that the choice was made. */
    fun confirmLanguage(language: AppLanguage) = setLanguage(language)

    fun completeOnboarding() = store.completeOnboarding()
}
