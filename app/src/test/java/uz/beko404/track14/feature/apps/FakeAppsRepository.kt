package uz.beko404.track14.feature.apps

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.PublishedApp
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.repository.AppsRepository
import uz.beko404.track14.domain.repository.PublishedRepository
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult

class FakeAppsRepository(initial: DomainResult<List<CatalogApp>>) : AppsRepository {
    val catalog = MutableStateFlow(initial)
    val requestedIds = mutableListOf<String>()

    override fun observeCatalog(): Flow<DomainResult<List<CatalogApp>>> = catalog

    override fun observeApp(id: String): Flow<DomainResult<CatalogApp>> {
        requestedIds += id
        return catalog.map { result ->
            when (result) {
                is DomainResult.Failure -> result
                is DomainResult.Success -> result.value.firstOrNull { it.id == id }
                    ?.let { DomainResult.Success(it) }
                    ?: DomainResult.Failure(DomainError.NotFound)
            }
        }
    }
}

fun testApp(
    id: String,
    category: String = "Ta'lim",
    createdAt: Long = 0,
    featuredUntil: Long = 0,
) = CatalogApp(
    id = id, name = id, category = category, testersJoined = 3, testersNeeded = 12,
    rewardPerDay = Coins(3), isPro = false, createdAtMillis = createdAt, featuredUntilMillis = featuredUntil,
)

class FakePublished : PublishedRepository {
    override fun observeApps(): Flow<DomainResult<List<PublishedApp>>> = MutableStateFlow(DomainResult.Success(emptyList()))
    override fun observeTotals(): Flow<PublishedTotals> = MutableStateFlow(PublishedTotals(count = 2, testers = 30))
}
