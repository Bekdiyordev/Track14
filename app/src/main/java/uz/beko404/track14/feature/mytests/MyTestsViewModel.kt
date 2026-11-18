package uz.beko404.track14.feature.mytests

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uz.beko404.track14.core.device.LaunchLog
import uz.beko404.track14.core.device.UsageAccess
import uz.beko404.track14.data.check.CheckOutcome
import uz.beko404.track14.data.check.DailyCheckRunner
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.TestsRepository
import uz.beko404.track14.domain.time.Clock
import javax.inject.Inject

sealed interface MyTestsUiState {
    data object Loading : MyTestsUiState
    data object SignedOut : MyTestsUiState
    data class Error(val error: DomainError) : MyTestsUiState
    data class Content(
        val active: List<TesterTest>,
        val finished: List<TesterTest>,
        val nowMillis: Long,
        val usageGranted: Boolean,
    ) : MyTestsUiState {
        val doneToday: Int get() = active.count { it.isDoneToday(nowMillis) }
    }
}

/** One-off messages for the snackbar. */
sealed interface MyTestsEvent {
    data class DayPassed(val appName: String, val day: Int, val completed: Boolean) : MyTestsEvent
    data class NotEnough(val appName: String, val seconds: Int) : MyTestsEvent
    data class Failed(val error: DomainError) : MyTestsEvent
    data class Left(val appName: String) : MyTestsEvent
}

@HiltViewModel
class MyTestsViewModel @Inject constructor(
    auth: AuthRepository,
    private val tests: TestsRepository,
    private val runner: DailyCheckRunner,
    private val launchLog: LaunchLog,
    private val usage: UsageAccess,
    private val clock: Clock,
) : ViewModel() {

    private val usageGranted = MutableStateFlow(usage.hasAccess())
    private val events = Channel<MyTestsEvent>(Channel.BUFFERED)
    val messages: Flow<MyTestsEvent> = events.receiveAsFlow()
    private val checking = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testsFlow: Flow<MyTestsUiState> = auth.session.flatMapLatest { session ->
        when (session) {
            Session.SignedOut -> flowOf(MyTestsUiState.SignedOut)
            is Session.SignedIn -> tests.observeMyTests(session.uid).map { result ->
                when (result) {
                    is DomainResult.Failure -> MyTestsUiState.Error(result.error)
                    is DomainResult.Success -> {
                        val (active, finished) = result.value.partition { it.status == TestStatus.ACTIVE }
                        MyTestsUiState.Content(active, finished, clock.nowMillis(), usageGranted = false)
                    }
                }
            }
        }
    }

    val state: StateFlow<MyTestsUiState> = combine(testsFlow, usageGranted) { s, granted ->
        if (s is MyTestsUiState.Content) s.copy(usageGranted = granted, nowMillis = clock.nowMillis()) else s
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyTestsUiState.Loading)

    /** Tester is about to open the tested app from Track14 (starts the mode B timer). */
    fun onOpenApp(test: TesterTest) {
        launchLog.recordLaunch(test.id, clock.nowMillis())
    }

    /** On every return to the screen: check the tests whose day is still open. */
    fun onResume() {
        usageGranted.value = usage.hasAccess()
        val content = state.value as? MyTestsUiState.Content ?: return
        viewModelScope.launch {
            if (!checking.tryLock()) return@launch
            try {
                for (test in content.active) {
                    when (val outcome = runner.check(test, allowFallback = true)) {
                        is CheckOutcome.Passed -> events.send(MyTestsEvent.DayPassed(test.appName, outcome.day, outcome.completed))
                        is CheckOutcome.NotEnough -> events.send(MyTestsEvent.NotEnough(test.appName, outcome.seconds))
                        is CheckOutcome.Failed -> events.send(MyTestsEvent.Failed(outcome.error))
                        CheckOutcome.NotNeeded -> Unit
                    }
                }
            } finally {
                checking.unlock()
            }
        }
    }

    fun leave(test: TesterTest) {
        viewModelScope.launch {
            checking.withLock {
                when (val result = tests.leaveTest(test.id)) {
                    is DomainResult.Success -> events.send(MyTestsEvent.Left(test.appName))
                    is DomainResult.Failure -> events.send(MyTestsEvent.Failed(result.error))
                }
            }
        }
    }
}
