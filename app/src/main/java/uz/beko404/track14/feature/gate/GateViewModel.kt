package uz.beko404.track14.feature.gate

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import uz.beko404.track14.domain.model.GateRules
import uz.beko404.track14.domain.model.GateState
import uz.beko404.track14.domain.model.SupportConfig
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.ConfigRepository
import uz.beko404.track14.domain.repository.Session
import javax.inject.Inject

/**
 * Decides, for the whole app, whether normal screens may be shown. The server enforces the
 * same rules (guards.ts), so this is about a clear screen, not about security.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GateViewModel @Inject constructor(
    @ApplicationContext context: Context,
    auth: AuthRepository,
    config: ConfigRepository,
) : ViewModel() {

    private val currentBuild: Long = runCatching {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    }.getOrDefault(Long.MAX_VALUE)

    private val prefs = context.getSharedPreferences("gate", Context.MODE_PRIVATE)
    private val dismissedBuild = MutableStateFlow(prefs.getLong(KEY_DISMISSED, 0L))

    private val blocked = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn) config.blockedReason(s.uid) else flowOf(null)
    }

    private var latestBuild = 0L

    val state: StateFlow<GateState> = combine(config.version(), config.flags(), blocked, dismissedBuild) { version, flags, reason, dismissed ->
        latestBuild = version.latestBuild
        when (val gate = GateRules.gate(currentBuild, version, flags, reason)) {
            is GateState.Open -> gate.copy(optionalUpdate = gate.optionalUpdate && dismissed < version.latestBuild)
            else -> gate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GateState.Loading)

    val support: StateFlow<SupportConfig> = config.support()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SupportConfig())

    /** "Later" on the optional update: not asked again until a newer build is published. */
    fun dismissOptionalUpdate() {
        prefs.edit().putLong(KEY_DISMISSED, latestBuild).apply()
        dismissedBuild.value = latestBuild
    }

    private companion object {
        const val KEY_DISMISSED = "dismissed_build"
    }
}
