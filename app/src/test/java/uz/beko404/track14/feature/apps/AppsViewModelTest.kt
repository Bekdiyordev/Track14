package uz.beko404.track14.feature.apps

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.rules.CatalogSort
import uz.beko404.track14.domain.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AppsViewModelTest {

    private val now = 1_000L
    private val apps = listOf(
        testApp("lugat", "Ta'lim", createdAt = 30, featuredUntil = 5_000),
        testApp("pul", "Moliya", createdAt = 20),
        testApp("kitob", "Ta'lim", createdAt = 10, featuredUntil = 500),
    )

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(repo: FakeAppsRepository): AppsViewModel {
        val vm = AppsViewModel(repo, Clock { now }, FakePublished())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun `shows catalog, categories and running promotions`() = runTest {
        val vm = viewModel(FakeAppsRepository(DomainResult.Success(apps)))
        val state = vm.state.value

        assertFalse(state.isLoading)
        assertEquals(listOf("lugat", "pul", "kitob"), state.apps.map { it.id })
        assertEquals(listOf("Moliya", "Ta'lim"), state.categories)
        assertEquals(listOf("lugat"), state.featured.map { it.id })
        assertEquals(3, state.totalCount)
    }

    @Test
    fun `category and search narrow the list and hide featured`() = runTest {
        val vm = viewModel(FakeAppsRepository(DomainResult.Success(apps)))

        vm.onCategorySelected("Ta'lim")
        assertEquals(listOf("lugat", "kitob"), vm.state.value.apps.map { it.id })
        assertTrue(vm.state.value.featured.isEmpty())

        vm.onQueryChange("kit")
        assertEquals(listOf("kitob"), vm.state.value.apps.map { it.id })

        vm.onCategorySelected(null)
        vm.onQueryChange("")
        assertEquals(3, vm.state.value.apps.size)
    }

    @Test
    fun `sort selection is kept in the filter`() = runTest {
        val vm = viewModel(FakeAppsRepository(DomainResult.Success(apps)))
        vm.onSortSelected(CatalogSort.RATING)
        assertEquals(CatalogSort.RATING, vm.filter.value.sort)
    }

    @Test
    fun `query is capped`() = runTest {
        val vm = viewModel(FakeAppsRepository(DomainResult.Success(apps)))
        vm.onQueryChange("x".repeat(500))
        assertEquals(60, vm.filter.value.query.length)
    }

    @Test
    fun `error is reported`() = runTest {
        val vm = viewModel(FakeAppsRepository(DomainResult.Failure(DomainError.Network)))
        assertEquals(DomainError.Network, vm.state.value.error)
        assertFalse(vm.state.value.isLoading)
    }
}
