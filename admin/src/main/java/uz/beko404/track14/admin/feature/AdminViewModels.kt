package uz.beko404.track14.admin.feature

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import uz.beko404.track14.admin.data.AdminApp
import uz.beko404.track14.admin.data.AdminRepository
import uz.beko404.track14.admin.data.AdminUser
import uz.beko404.track14.admin.data.AppDetailAdmin
import uz.beko404.track14.admin.data.AuditEntry
import uz.beko404.track14.admin.data.DashboardCounts
import uz.beko404.track14.admin.data.PushJob
import uz.beko404.track14.admin.data.Report
import uz.beko404.track14.admin.data.UserDetail
import uz.beko404.track14.admin.logic.AdminRules
import uz.beko404.track14.admin.logic.Role
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import javax.inject.Inject

/** Common screen state: data, a loading flag and the last error or success message. */
data class Screen<T>(
    val data: T? = null,
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: DomainError? = null,
    val done: String? = null,
)

/** Small base: load() into a Screen, and run actions that reload afterwards. */
abstract class LoadingViewModel<T> : ViewModel() {
    protected val _state = MutableStateFlow(Screen<T>())
    val state: StateFlow<Screen<T>> = _state.asStateFlow()

    protected abstract suspend fun fetch(): DomainResult<T>

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val r = fetch()) {
                is DomainResult.Success -> _state.update { it.copy(loading = false, data = r.value) }
                is DomainResult.Failure -> _state.update { it.copy(loading = false, error = r.error) }
            }
        }
    }

    protected fun act(doneMessage: String, block: suspend () -> DomainResult<Unit>) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null, done = null) }
        viewModelScope.launch {
            when (val r = block()) {
                is DomainResult.Success -> {
                    _state.update { it.copy(busy = false, done = doneMessage) }
                    load()
                }
                is DomainResult.Failure -> _state.update { it.copy(busy = false, error = r.error) }
            }
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, done = null) }

    protected fun fail(error: DomainError) = _state.update { it.copy(error = error) }
}

// ---------------------------------------------------------------- session

sealed interface AdminSession {
    data object Checking : AdminSession
    data object SignedOut : AdminSession
    data object NotStaff : AdminSession
    data object CheckFailed : AdminSession
    data class Staff(val role: Role) : AdminSession
}

@HiltViewModel
class SessionViewModel @Inject constructor(private val repo: AdminRepository) : ViewModel() {
    private val _session = MutableStateFlow<AdminSession>(AdminSession.Checking)
    val session: StateFlow<AdminSession> = _session.asStateFlow()
    private var roleCheck: Job? = null

    init {
        viewModelScope.launch {
            repo.signedInUser.collect { checkRole(forceRefresh = false) }
        }
    }

    fun refresh() = checkRole(forceRefresh = true)

    private fun checkRole(forceRefresh: Boolean) {
        roleCheck?.cancel()
        roleCheck = viewModelScope.launch {
            if (!repo.isSignedIn()) {
                _session.value = AdminSession.SignedOut
                return@launch
            }
            _session.value = AdminSession.Checking
            _session.value = try {
                val role = withTimeout(60_000) { repo.currentRole(forceRefresh) }
                if (role == null) AdminSession.NotStaff else AdminSession.Staff(role)
            } catch (_: TimeoutCancellationException) {
                AdminSession.CheckFailed
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                AdminSession.CheckFailed
            }
        }
    }

    fun signOut() {
        roleCheck?.cancel()
        repo.signOut()
        _session.value = AdminSession.SignedOut
    }
}

// ---------------------------------------------------------------- dashboard, lists

@HiltViewModel
class DashboardViewModel @Inject constructor(private val repo: AdminRepository) : LoadingViewModel<DashboardCounts>() {
    override suspend fun fetch() = repo.dashboard()

    /** Superadmin only (adminSetConfig); the screen asks for confirmation first. */
    fun setMaintenance(on: Boolean) {
        val flags = state.value.data?.flags ?: return
        act(if (on) "Texnik ishlar rejimi yoqildi" else "Texnik ishlar rejimi o'chirildi") { repo.setMaintenance(on, flags) }
    }
}

@HiltViewModel
class UsersViewModel @Inject constructor(private val repo: AdminRepository) : LoadingViewModel<List<AdminUser>>() {
    val query = MutableStateFlow("")
    override suspend fun fetch() = repo.users(query.value)
}

@HiltViewModel
class AppsViewModel @Inject constructor(private val repo: AdminRepository) : LoadingViewModel<List<AdminApp>>() {
    val filter = MutableStateFlow<AppStatus?>(null)
    override suspend fun fetch() = repo.apps(filter.value)
    fun setFilter(s: AppStatus?) {
        filter.value = s
        load()
    }
}

@HiltViewModel
class ReportsViewModel @Inject constructor(private val repo: AdminRepository) : LoadingViewModel<List<Report>>() {
    val tab = MutableStateFlow("open")
    override suspend fun fetch() = repo.reports(tab.value)
    fun setTab(t: String) {
        tab.value = t
        load()
    }
    fun resolve(id: String, status: String, note: String) =
        act(if (status == "resolved") "Shikoyat yopildi" else "Shikoyat rad etildi") { repo.resolveReport(id, status, note) }

    /** Set once the uid behind a tester report is known; the screen opens that user. */
    val openUser = MutableStateFlow<String?>(null)

    fun openTester(testId: String) {
        viewModelScope.launch {
            when (val r = repo.testerOf(testId)) {
                is DomainResult.Success -> openUser.value = r.value
                is DomainResult.Failure -> fail(r.error)
            }
        }
    }
}

@HiltViewModel
class AuditViewModel @Inject constructor(private val repo: AdminRepository) : LoadingViewModel<List<AuditEntry>>() {
    override suspend fun fetch() = repo.audit()
}

// ---------------------------------------------------------------- user and app detail

@HiltViewModel
class UserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AdminRepository,
) : LoadingViewModel<UserDetail>() {
    private val uid: String = savedStateHandle.get<String>(ARG_ID).orEmpty()

    override suspend fun fetch(): DomainResult<UserDetail> =
        if (AdminRules.isUid(uid)) repo.user(uid) else DomainResult.Failure(DomainError.NotFound)

    /** [days] 7 or 30, null = permanent. */
    fun block(reason: String, days: Int?) = act("Bloklandi") { repo.blockUser(uid, true, reason, days) }

    /** Set after a superadmin deleted the account; the screen goes back. */
    val deleted = MutableStateFlow(false)

    fun delete() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            when (val r = repo.deleteUser(uid)) {
                is DomainResult.Success -> deleted.value = true
                is DomainResult.Failure -> _state.update { it.copy(busy = false, error = r.error) }
            }
        }
    }
    fun unblock() = act("Blok olib tashlandi") { repo.blockUser(uid, false, "") }
    fun setRole(role: Role?) = act("Rol saqlandi") { repo.setRole(uid, role) }

    fun adjust(amountText: String, reason: String) {
        val amount = AdminRules.adjustAmount(amountText)
        if (amount == null || !AdminRules.isReason(reason)) return fail(DomainError.Rejected("invalid_input"))
        act("Balans tuzatildi") { repo.adjustCoins(uid, amount, reason) }
    }
}

@HiltViewModel
class AppViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AdminRepository,
) : LoadingViewModel<AppDetailAdmin>() {
    private val appId: String = savedStateHandle.get<String>(ARG_ID).orEmpty()

    override suspend fun fetch(): DomainResult<AppDetailAdmin> =
        if (Regex("^[A-Za-z0-9_-]{1,64}$").matches(appId)) repo.app(appId) else DomainResult.Failure(DomainError.NotFound)

    fun suspend(reason: String, refundFee: Boolean) = act("Ilova bloklandi") { repo.suspendApp(appId, true, reason, refundFee) }
    fun restore() = act("Ilova tiklandi") { repo.suspendApp(appId, false, "", false) }

    /** "Rad etish": every open report on this app is dismissed with the same note. */
    fun dismissReports(note: String) {
        val open = state.value.data?.reports.orEmpty().filter { it.status == "open" }
        if (open.isEmpty()) return
        act("${open.size} ta shikoyat rad etildi") {
            open.map { repo.resolveReport(it.id, "dismissed", note) }.firstOrNull { it is DomainResult.Failure } ?: DomainResult.Success(Unit)
        }
    }
}

// ---------------------------------------------------------------- push

data class PushForm(
    val toAll: Boolean = true,
    val uids: String = "",
    val title: String = "",
    val body: String = "",
    /** null = send now; otherwise epoch millis. */
    val sendAt: Long? = null,
)

@HiltViewModel
class PushViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AdminRepository,
) : LoadingViewModel<List<PushJob>>() {
    /** Opened from a user's page: the form starts addressed to that one uid. */
    private val prefillUid: String? = savedStateHandle.get<String>(ARG_ID)?.takeIf { AdminRules.isUid(it) }
    val form = MutableStateFlow(prefillUid?.let { PushForm(toAll = false, uids = it) } ?: PushForm())
    override suspend fun fetch() = repo.pushJobs()

    fun update(change: (PushForm) -> PushForm) = form.update(change)

    fun send(nowMillis: Long) {
        val f = form.value
        val uids = if (f.toAll) null else AdminRules.parseUids(f.uids)
        val valid = AdminRules.isPushTitle(f.title) && AdminRules.isPushBody(f.body) &&
            (f.toAll || uids != null) && AdminRules.isValidSendAt(f.sendAt, nowMillis)
        if (!valid) return fail(DomainError.Rejected("invalid_input"))
        act(if (f.sendAt == null) "Yuborildi (1 daqiqa ichida)" else "Rejalashtirildi") {
            repo.sendPush(uids, f.title, f.body, f.sendAt).also { if (it is DomainResult.Success) form.value = PushForm() }
        }
    }

    fun cancel(jobId: String) = act("Bekor qilindi") { repo.cancelPush(jobId) }
}

// ---------------------------------------------------------------- config documents

/** One editor for `config/{doc}`: values are edited as text and converted on save. */
@HiltViewModel
class ConfigViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: AdminRepository,
) : LoadingViewModel<Map<String, String>>() {
    val doc: String = savedStateHandle.get<String>(ARG_ID).orEmpty()
    val fields = MutableStateFlow<Map<String, String>>(emptyMap())

    override suspend fun fetch(): DomainResult<Map<String, String>> {
        if (doc !in DOCS) return DomainResult.Failure(DomainError.NotFound)
        val r = repo.config(doc)
        if (r is DomainResult.Success) fields.value = r.value.mapValues { (_, v) -> v?.toString().orEmpty() }
        return when (r) {
            is DomainResult.Success -> DomainResult.Success(fields.value)
            is DomainResult.Failure -> r
        }
    }

    fun set(key: String, value: String) = fields.update { it + (key to value) }

    fun save() {
        val f = fields.value
        val value: Map<String, Any>? = when (doc) {
            "app_version" -> {
                val min = f["minBuild"]?.trim()?.toLongOrNull()
                val latest = f["latestBuild"]?.trim()?.toLongOrNull()
                if (!AdminRules.isValidVersion(min, latest)) null
                else buildMap {
                    put("minBuild", min!!)
                    put("latestBuild", latest!!)
                    f["message"]?.trim()?.takeIf { it.isNotEmpty() }?.let { put("message", it.take(300)) }
                }
            }
            "support" -> {
                val tg = f["telegram"].orEmpty().trim()
                val email = f["email"].orEmpty().trim()
                val group = f["groupUrl"].orEmpty().trim()
                val faq = f["faqUrl"].orEmpty().trim()
                if (!AdminRules.isTelegram(tg) || !AdminRules.isEmail(email) || !AdminRules.isGroupUrl(group) || !AdminRules.isHttps(faq)) null
                else buildMap {
                    if (tg.isNotEmpty()) put("telegram", tg)
                    if (email.isNotEmpty()) put("email", email)
                    if (group.isNotEmpty()) put("groupUrl", group)
                    if (faq.isNotEmpty()) put("faqUrl", faq)
                }
            }
            "flags" -> buildMap {
                put("maintenance", f["maintenance"] == "true")
                put("newApps", f["newApps"] != "false")
                put("purchases", f["purchases"] != "false")
                f["maintenanceMessage"]?.trim()?.takeIf { it.isNotEmpty() }?.let { put("maintenanceMessage", it.take(300)) }
            }
            "economy" -> AdminRules.parseEconomy(f)
            else -> null
        }
        if (value == null) return fail(DomainError.Rejected("invalid_input"))
        act("Saqlandi") { repo.setConfig(doc, value) }
    }

    companion object {
        val DOCS = listOf("app_version", "support", "flags", "economy")
    }
}

const val ARG_ID = "id"
