package uz.beko404.track14.feature.appdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import uz.beko404.track14.domain.model.AppIds
import uz.beko404.track14.domain.repository.AppsRepository
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import javax.inject.Inject

sealed interface AppDetailUiState {
    data object Loading : AppDetailUiState
    data object NotFound : AppDetailUiState
    data class Error(val error: DomainError) : AppDetailUiState
    data class Content(val app: CatalogApp) : AppDetailUiState
}

@HiltViewModel
class AppDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: AppsRepository,
) : ViewModel() {

    /** Untrusted navigation input: an invalid id never reaches Firestore. */
    private val appId: String? = savedStateHandle.get<String>(ARG_APP_ID)?.takeIf { AppIds.isValid(it) }

    val state: StateFlow<AppDetailUiState> =
        (appId?.let { id -> repository.observeApp(id).map(::toUiState) } ?: flowOf(AppDetailUiState.NotFound))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppDetailUiState.Loading)

    private fun toUiState(result: DomainResult<CatalogApp>): AppDetailUiState = when (result) {
        is DomainResult.Success -> AppDetailUiState.Content(result.value)
        is DomainResult.Failure ->
            if (result.error == DomainError.NotFound) AppDetailUiState.NotFound else AppDetailUiState.Error(result.error)
    }

    companion object {
        const val ARG_APP_ID = "appId"
    }
}
