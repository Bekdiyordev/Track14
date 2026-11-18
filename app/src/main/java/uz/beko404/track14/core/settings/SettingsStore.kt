package uz.beko404.track14.core.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class AppLanguage(val tag: String) { UZ("uz"), RU("ru"), EN("en") }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val languageChosen: Boolean = false,
    val onboardingDone: Boolean = false,
)

/** Local preferences (not backed up, see data_extraction_rules). */
@Singleton
class SettingsStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        Settings(
            theme = runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, null) ?: "") }.getOrDefault(ThemeMode.SYSTEM),
            languageChosen = prefs.getBoolean(KEY_LANGUAGE_CHOSEN, false),
            onboardingDone = prefs.getBoolean(KEY_ONBOARDING, false),
        ),
    )
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    fun setTheme(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.value = _settings.value.copy(theme = mode)
    }

    /** Applies at once; AppCompat recreates the activity in the new language. */
    fun setLanguage(language: AppLanguage) {
        prefs.edit().putBoolean(KEY_LANGUAGE_CHOSEN, true).apply()
        _settings.value = _settings.value.copy(languageChosen = true)
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun currentLanguage(): AppLanguage {
        val tag = AppCompatDelegate.getApplicationLocales()[0]?.language
            ?: java.util.Locale.getDefault().language
        return AppLanguage.entries.firstOrNull { it.tag == tag } ?: AppLanguage.UZ
    }

    fun completeOnboarding() {
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
        _settings.value = _settings.value.copy(onboardingDone = true)
    }

    private companion object {
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE_CHOSEN = "language_chosen"
        const val KEY_ONBOARDING = "onboarding_done"
    }
}
