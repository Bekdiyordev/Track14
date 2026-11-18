package uz.beko404.track14.feature.jointest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.beko404.track14.core.device.UsageAccess
import uz.beko404.track14.domain.model.AppIds
import uz.beko404.track14.domain.repository.AppsRepository
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.TestsRepository
import java.time.ZoneId
import javax.inject.Inject

/** Steps 1-3 cannot be verified by the app (Google Group, Play opt-in, install); the tester confirms them. */
data class JoinSteps(
    val joinedGroup: Boolean = false,
    val optedIn: Boolean = false,
    val installed: Boolean = false,
    val usageGranted: Boolean = false,
    val isStarting: Boolean = false,
    val error: DomainError? = null,
    val startedTestId: String? = null,
)

data class JoinTestUiState(
    val loading: Boolean = true,
    val app: CatalogApp? = null,
    val steps: JoinSteps = JoinSteps(),
) {
    /** Usage access is recommended, not required: without it the fallback check (mode B) is used. */
    val canStart: Boolean
        get() = app != null && steps.joinedGroup && steps.optedIn && steps.installed && !steps.isStarting
}

@HiltViewModel
class JoinTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    apps: AppsRepository,
    private val tests: TestsRepository,
    private val usage: UsageAccess,
) : ViewModel() {

    private val campaignId: String? = savedStateHandle.get<String>(ARG_CAMPAIGN_ID)?.takeIf { AppIds.isValid(it) }
    private val steps = MutableStateFlow(JoinSteps(usageGranted = usage.hasAccess()))

    val state: StateFlow<JoinTestUiState> = combine(
        campaignId?.let(apps::observeApp) ?: flowOf(DomainResult.Failure(DomainError.NotFound)),
        steps,
    ) { app, steps ->
        JoinTestUiState(loading = false, app = (app as? DomainResult.Success)?.value, steps = steps)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JoinTestUiState())

    fun onGroupOpened() = steps.update { it.copy(joinedGroup = true) }
    fun onOptInOpened() = steps.update { it.copy(optedIn = true) }
    fun onStoreOpened() = steps.update { it.copy(installed = true) }

    /** Called on every resume: the tester may have just granted the permission in Settings. */
    fun refreshUsageAccess() = steps.update { it.copy(usageGranted = usage.hasAccess()) }

    fun dismissError() = steps.update { it.copy(error = null) }

    fun start() {
        val id = campaignId ?: return
        if (!state.value.canStart) return
        steps.update { it.copy(isStarting = true, error = null) }
        viewModelScope.launch {
            when (val result = tests.startTest(id, ZoneId.systemDefault().id)) {
                is DomainResult.Success -> steps.update { it.copy(isStarting = false, startedTestId = result.value) }
                is DomainResult.Failure -> steps.update { it.copy(isStarting = false, error = result.error) }
            }
        }
    }

    companion object {
        const val ARG_CAMPAIGN_ID = "campaignId"
    }
}
