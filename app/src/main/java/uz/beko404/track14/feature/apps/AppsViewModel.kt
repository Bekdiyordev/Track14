package uz.beko404.track14.feature.apps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.repository.AppsRepository
import uz.beko404.track14.domain.repository.PublishedRepository
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.rules.CatalogFilter
import uz.beko404.track14.domain.rules.CatalogRules
import uz.beko404.track14.domain.rules.CatalogSort
import uz.beko404.track14.domain.time.Clock
import javax.inject.Inject

data class AppsUiState(
    val isLoading: Boolean = true,
    val error: DomainError? = null,
    val categories: List<String> = emptyList(),
    val featured: List<CatalogApp> = emptyList(),
    val apps: List<CatalogApp> = emptyList(),
    /** Size of the whole catalog, before search and filters. */
    val totalCount: Int = 0,
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    repository: AppsRepository,
    private val clock: Clock,
    published: PublishedRepository,
) : ViewModel() {

    /** "N apps shipped through Track14" banner. */
    val publishedTotals: StateFlow<PublishedTotals> = published.observeTotals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PublishedTotals())


    private val _filter = MutableStateFlow(CatalogFilter())

    /** Exposed on its own so the search field updates without waiting for the list. */
    val filter: StateFlow<CatalogFilter> = _filter.asStateFlow()

    val state: StateFlow<AppsUiState> = combine(repository.observeCatalog(), _filter, ::reduce)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppsUiState())

    fun onQueryChange(query: String) {
        _filter.update { it.copy(query = query.take(MAX_QUERY_LENGTH)) }
    }

    fun onCategorySelected(category: String?) {
        _filter.update { it.copy(category = category) }
    }

    fun onSortSelected(sort: CatalogSort) {
        _filter.update { it.copy(sort = sort) }
    }

    private fun reduce(result: DomainResult<List<CatalogApp>>, filter: CatalogFilter): AppsUiState =
        when (result) {
            is DomainResult.Failure -> AppsUiState(isLoading = false, error = result.error)
            is DomainResult.Success -> {
                val all = result.value
                AppsUiState(
                    isLoading = false,
                    categories = CatalogRules.categories(all),
                    featured = if (filter.isBrowsing) CatalogRules.featured(all, clock.nowMillis()) else emptyList(),
                    apps = CatalogRules.apply(all, filter),
                    totalCount = all.size,
                )
            }
        }

    private companion object {
        const val MAX_QUERY_LENGTH = 60
    }
}
