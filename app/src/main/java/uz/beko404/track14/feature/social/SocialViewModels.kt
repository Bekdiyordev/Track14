package uz.beko404.track14.feature.social

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.beko404.track14.domain.model.AppNotification
import uz.beko404.track14.domain.model.FeedbackRules
import uz.beko404.track14.domain.model.FeedbackTag
import uz.beko404.track14.domain.model.NewFeedback
import uz.beko404.track14.domain.model.ReportReason
import uz.beko404.track14.domain.model.ReportTarget
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.FeedbackRepository
import uz.beko404.track14.domain.repository.NotificationsRepository
import uz.beko404.track14.domain.repository.ReportRepository
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.TestsRepository
import javax.inject.Inject

private val TEST_ID = Regex("^[A-Za-z0-9_-]{1,160}$")

// ---------------------------------------------------------------- feedback

data class FeedbackForm(
    val rating: Int = 0,
    val tags: Set<FeedbackTag> = emptySet(),
    val text: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: DomainError? = null,
)

data class FeedbackUiState(val test: TesterTest? = null, val form: FeedbackForm = FeedbackForm()) {
    val canSend: Boolean get() = test != null && !form.sending && FeedbackRules.isValid(NewFeedback(form.rating, form.tags, form.text))
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    tests: TestsRepository,
    private val feedback: FeedbackRepository,
) : ViewModel() {
    private val testId: String? = savedStateHandle.get<String>(ARG_TEST_ID)?.takeIf { TEST_ID.matches(it) }
    private val form = MutableStateFlow(FeedbackForm())

    private val test = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn && testId != null) {
            tests.observeMyTests(s.uid).map { r -> (r as? DomainResult.Success)?.value?.firstOrNull { it.id == testId } }
        } else {
            flowOf(null)
        }
    }

    val state: StateFlow<FeedbackUiState> = combine(test, form, ::FeedbackUiState)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedbackUiState())

    fun setRating(r: Int) = form.update { it.copy(rating = r.coerceIn(1, 5)) }
    fun toggle(tag: FeedbackTag) = form.update { f ->
        val tags = if (tag in f.tags) f.tags - tag else f.tags + tag
        f.copy(tags = tags.take(FeedbackRules.MAX_TAGS).toSet())
    }
    fun setText(t: String) = form.update { it.copy(text = t.take(FeedbackRules.MAX_TEXT)) }

    fun send() {
        val s = state.value
        val t = s.test ?: return
        if (!s.canSend) return
        form.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val session = auth.session.first() as? Session.SignedIn
            val r = if (session == null) {
                DomainResult.Failure(DomainError.Unauthenticated)
            } else {
                feedback.submit(session.uid, t, NewFeedback(s.form.rating, s.form.tags, s.form.text))
            }
            form.update { it.copy(sending = false, sent = r is DomainResult.Success, error = (r as? DomainResult.Failure)?.error) }
        }
    }

    companion object {
        const val ARG_TEST_ID = "testId"
    }
}

// ---------------------------------------------------------------- report

data class ReportUiState(
    val target: ReportTarget? = null,
    val reason: ReportReason? = null,
    val comment: String = "",
    val sending: Boolean = false,
    val sent: Boolean = false,
    val error: DomainError? = null,
) {
    val reasons: List<ReportReason> get() = target?.let(ReportReason::forTarget).orEmpty()
    val canSend: Boolean get() = target != null && reason != null && !sending
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val reports: ReportRepository,
) : ViewModel() {
    private val targetId: String? = savedStateHandle.get<String>(ARG_TARGET_ID)?.takeIf { TEST_ID.matches(it) }
    private val _state = MutableStateFlow(
        ReportUiState(target = ReportTarget.entries.firstOrNull { it.wireName == savedStateHandle.get<String>(ARG_TARGET_TYPE) }),
    )
    val state: StateFlow<ReportUiState> = _state.asStateFlow()

    fun pick(reason: ReportReason) = _state.update { it.copy(reason = reason) }
    fun setComment(c: String) = _state.update { it.copy(comment = c.take(1_000)) }

    fun send() {
        val s = _state.value
        val target = s.target ?: return
        val reason = s.reason ?: return
        val id = targetId ?: return
        if (s.sending) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            val session = auth.session.first() as? Session.SignedIn
            val r = if (session == null) {
                DomainResult.Failure(DomainError.Unauthenticated)
            } else {
                reports.submit(session.uid, target, id, reason, s.comment)
            }
            _state.update { it.copy(sending = false, sent = r is DomainResult.Success, error = (r as? DomainResult.Failure)?.error) }
        }
    }

    companion object {
        const val ARG_TARGET_TYPE = "targetType"
        const val ARG_TARGET_ID = "targetId"
    }
}

// ---------------------------------------------------------------- notifications

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val notifications: NotificationsRepository,
) : ViewModel() {
    val items: StateFlow<List<AppNotification>?> = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn) notifications.observe(s.uid).map { (it as? DomainResult.Success)?.value.orEmpty() } else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun open(n: AppNotification) {
        if (n.read) return
        viewModelScope.launch {
            (auth.session.first() as? Session.SignedIn)?.let { notifications.markRead(it.uid, n.id) }
        }
    }
}
