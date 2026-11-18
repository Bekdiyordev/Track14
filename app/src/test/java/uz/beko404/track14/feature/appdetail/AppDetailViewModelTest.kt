package uz.beko404.track14.feature.appdetail

import androidx.lifecycle.SavedStateHandle
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.feature.apps.FakeAppsRepository
import uz.beko404.track14.feature.apps.testApp

@OptIn(ExperimentalCoroutinesApi::class)
class AppDetailViewModelTest {

    private val repo = FakeAppsRepository(DomainResult.Success(listOf(testApp("lugat"))))

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(id: String?): AppDetailViewModel {
        val handle = SavedStateHandle(if (id == null) emptyMap() else mapOf(AppDetailViewModel.ARG_APP_ID to id))
        val vm = AppDetailViewModel(handle, repo)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test
    fun `known app is shown`() = runTest {
        val state = viewModel("lugat").state.value
        assertTrue(state is AppDetailUiState.Content)
        assertEquals("lugat", (state as AppDetailUiState.Content).app.id)
    }

    @Test
    fun `unknown app is not found`() = runTest {
        assertEquals(AppDetailUiState.NotFound, viewModel("yoq").state.value)
    }

    @Test
    fun `invalid or missing ids never reach the repository`() = runTest {
        assertEquals(AppDetailUiState.NotFound, viewModel("../wallets/u1").state.value)
        assertEquals(AppDetailUiState.NotFound, viewModel(null).state.value)
        assertTrue(repo.requestedIds.isEmpty())
    }
}
