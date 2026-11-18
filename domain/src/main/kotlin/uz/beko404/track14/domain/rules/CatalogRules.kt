package uz.beko404.track14.domain.rules

import uz.beko404.track14.domain.repository.CatalogApp

/** Sort options of the catalog (plan: by time, by rating, apps that need testers most). */
enum class CatalogSort { NEWEST, RATING, FEW_TESTERS }

data class CatalogFilter(
    val query: String = "",
    val category: String? = null,
    val sort: CatalogSort = CatalogSort.NEWEST,
) {
    val isBrowsing: Boolean get() = query.isBlank() && category == null
}

/** Search, category filter and sorting of the catalog. Pure and deterministic. */
object CatalogRules {

    fun apply(apps: List<CatalogApp>, filter: CatalogFilter): List<CatalogApp> {
        val query = filter.query.trim()
        return apps
            .filter { filter.category == null || it.category == filter.category }
            .filter {
                query.isEmpty() ||
                    it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
            .sortedWith(comparator(filter.sort))
    }

    /** Apps with an active top promotion, the latest-ending promotion first. */
    fun featured(apps: List<CatalogApp>, nowMillis: Long): List<CatalogApp> =
        apps.filter { it.featuredUntilMillis > nowMillis }
            .sortedByDescending { it.featuredUntilMillis }

    fun categories(apps: List<CatalogApp>): List<String> =
        apps.map { it.category }.filter { it.isNotBlank() }.distinct().sorted()

    private fun comparator(sort: CatalogSort): Comparator<CatalogApp> {
        val newestFirst = compareByDescending<CatalogApp> { it.createdAtMillis }.thenBy { it.id }
        return when (sort) {
            CatalogSort.NEWEST -> newestFirst
            CatalogSort.RATING ->
                compareByDescending<CatalogApp> { it.rating ?: -1.0 }.then(newestFirst)
            CatalogSort.FEW_TESTERS ->
                compareBy<CatalogApp> { it.testersProgress }.then(newestFirst)
        }
    }
}
