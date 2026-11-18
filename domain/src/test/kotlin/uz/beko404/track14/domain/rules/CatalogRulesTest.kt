package uz.beko404.track14.domain.rules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.repository.CatalogApp

class CatalogRulesTest {

    private fun app(
        id: String,
        name: String = id,
        category: String = "Ta'lim",
        joined: Int = 0,
        rating: Double? = null,
        createdAt: Long = 0,
        featuredUntil: Long = 0,
    ) = CatalogApp(
        id = id, name = name, category = category, testersJoined = joined, testersNeeded = 12,
        rewardPerDay = Coins(3), isPro = false, rating = rating, createdAtMillis = createdAt,
        featuredUntilMillis = featuredUntil,
    )

    private val apps = listOf(
        app("lugat", "Lug'at Pro", "Ta'lim", joined = 9, rating = 4.6, createdAt = 300),
        app("pul", "Pul Hisob", "Moliya", joined = 2, rating = 4.8, createdAt = 100),
        app("qadam", "Qadam", "Sog'liq", joined = 12, rating = null, createdAt = 200),
    )

    private fun ids(list: List<CatalogApp>) = list.map { it.id }

    @Test
    fun `newest first by default`() {
        assertEquals(listOf("lugat", "qadam", "pul"), ids(CatalogRules.apply(apps, CatalogFilter())))
    }

    @Test
    fun `rating puts unrated apps last`() {
        val sorted = CatalogRules.apply(apps, CatalogFilter(sort = CatalogSort.RATING))
        assertEquals(listOf("pul", "lugat", "qadam"), ids(sorted))
    }

    @Test
    fun `few testers puts apps that need testers most first`() {
        val sorted = CatalogRules.apply(apps, CatalogFilter(sort = CatalogSort.FEW_TESTERS))
        assertEquals(listOf("pul", "lugat", "qadam"), ids(sorted))
    }

    @Test
    fun `category and query filter, case insensitive`() {
        assertEquals(listOf("pul"), ids(CatalogRules.apply(apps, CatalogFilter(category = "Moliya"))))
        assertEquals(listOf("lugat"), ids(CatalogRules.apply(apps, CatalogFilter(query = "  lug'AT "))))
        assertEquals(listOf("qadam"), ids(CatalogRules.apply(apps, CatalogFilter(query = "sog"))))
        assertTrue(CatalogRules.apply(apps, CatalogFilter(query = "yo'q")).isEmpty())
    }

    @Test
    fun `featured only while the promotion is running`() {
        val list = listOf(
            app("a", featuredUntil = 500),
            app("b", featuredUntil = 900),
            app("c", featuredUntil = 100),
        )
        assertEquals(listOf("b", "a"), ids(CatalogRules.featured(list, nowMillis = 200)))
    }

    @Test
    fun `categories are distinct, sorted and non blank`() {
        val list = apps + app("x", category = "Moliya") + app("y", category = " ")
        assertEquals(listOf("Moliya", "Sog'liq", "Ta'lim"), CatalogRules.categories(list))
    }

    @Test
    fun `browsing means no query and no category`() {
        assertTrue(CatalogFilter(query = "  ").isBrowsing)
        assertFalse(CatalogFilter(category = "Moliya").isBrowsing)
    }
}
