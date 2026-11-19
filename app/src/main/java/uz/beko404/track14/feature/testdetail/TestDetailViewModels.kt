package uz.beko404.track14.feature.testdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.beko404.track14.domain.model.AppIds
import uz.beko404.track14.domain.model.CampaignTester
import uz.beko404.track14.domain.model.DayRecord
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.FeedbackRepository
import uz.beko404.track14.domain.repository.OwnerRepository
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.TestsRepository
import uz.beko404.track14.domain.time.Clock
import javax.inject.Inject

/** Test ids are `<campaignId>_<uid>`: same character set as app ids, a little longer. */
internal fun isTestId(id: String?): Boolean = id != null && id.length in 3..160 && id.all { it.isLetterOrDigit() || it == '_' || it == '-' }

sealed interface DetailState<out T> {
    data object Loading : DetailState<Nothing>
    data object NotFound : DetailState<Nothing>
    data class Error(val error: DomainError) : DetailState<Nothing>
    data class Content<T>(val value: T) : DetailState<T>
}

// ---------------------------------------------------------------- tester: one of my tests (screen 9)

data class TestDetail(val test: TesterTest, val records: List<DayRecord>, val nowMillis: Long)

sealed interface TestDetailEvent {
    data object Left : TestDetailEvent
    data class Failed(val error: DomainError) : TestDetailEvent
}

@HiltViewModel
class TestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    auth: AuthRepository,
    private val tests: TestsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val testId: String? = savedStateHandle.get<String>(ARG_TEST_ID)?.takeIf(::isTestId)
    private val events = Channel<TestDetailEvent>(Channel.BUFFERED)
    val messages: Flow<TestDetailEvent> = events.receiveAsFlow()
    private val leaving = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = leaving

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DetailState<TestDetail>> = auth.session.flatMapLatest { session ->
        val id = testId
        if (session !is Session.SignedIn || id == null) {
            flowOf(DetailState.NotFound)
        } else {
            combine(tests.observeMyTests(session.uid), tests.observeDayRecords(id)) { result, records ->
                when (result) {
                    is DomainResult.Failure -> DetailState.Error(result.error)
                    is DomainResult.Success -> result.value.firstOrNull { it.id == id }
                        ?.let { DetailState.Content(TestDetail(it, records, clock.nowMillis())) }
                        ?: DetailState.NotFound
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    fun leave() {
        val id = testId ?: return
        if (leaving.value) return
        leaving.value = true
        viewModelScope.launch {
            when (val r = tests.leaveTest(id)) {
                is DomainResult.Success -> events.send(TestDetailEvent.Left)
                is DomainResult.Failure -> events.send(TestDetailEvent.Failed(r.error))
            }
            leaving.value = false
        }
    }

    companion object {
        const val ARG_TEST_ID = "testId"
    }
}

// ---------------------------------------------------------------- owner: one tester of my app (screen 14)

data class TesterDetail(
    val tester: CampaignTester,
    val records: List<DayRecord>,
    val feedback: List<Feedback>,
    val appName: String,
)

@HiltViewModel
class TesterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    auth: AuthRepository,
    owner: OwnerRepository,
    tests: TestsRepository,
    feedbackRepository: FeedbackRepository,
) : ViewModel() {
    private val appId: String? = savedStateHandle.get<String>(ARG_APP_ID)?.takeIf { AppIds.isValid(it) }
    private val testId: String? = savedStateHandle.get<String>(ARG_TEST_ID)?.takeIf(::isTestId)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DetailState<TesterDetail>> = auth.session.flatMapLatest { session ->
        val app = appId
        val test = testId
        if (session !is Session.SignedIn || app == null || test == null) {
            flowOf(DetailState.NotFound)
        } else {
            val appName = owner.observeMyApps(session.uid).map { r ->
                (r as? DomainResult.Success)?.value?.firstOrNull { it.id == app }?.name.orEmpty()
            }
            val feedback = feedbackRepository.observeForApp(session.uid, app).map { r ->
                (r as? DomainResult.Success)?.value.orEmpty().filter { it.testId == test }
            }
            combine(owner.observeTesters(session.uid, app), tests.observeDayRecords(test), feedback, appName) { testers, records, fb, name ->
                when (testers) {
                    is DomainResult.Failure -> DetailState.Error(testers.error)
                    is DomainResult.Success -> testers.value.firstOrNull { it.testId == test }
                        ?.let { DetailState.Content(TesterDetail(it, records, fb, name)) }
                        ?: DetailState.NotFound
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailState.Loading)

    companion object {
        const val ARG_APP_ID = "appId"
        const val ARG_TEST_ID = "testId"
    }
}
