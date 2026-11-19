package uz.beko404.track14.feature.myapps

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
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
import uz.beko404.track14.core.device.ApkReadResult
import uz.beko404.track14.core.device.ApkReader
import uz.beko404.track14.domain.model.AppIds
import uz.beko404.track14.domain.model.CampaignTester
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.EconomyConfig
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.OwnerAction
import uz.beko404.track14.domain.model.OwnerApp
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.EconomyRepository
import uz.beko404.track14.domain.repository.FeedbackRepository
import uz.beko404.track14.domain.repository.OwnerRepository
import uz.beko404.track14.domain.repository.Session
import uz.beko404.track14.domain.repository.WalletRepository
import uz.beko404.track14.domain.rules.NewApp
import uz.beko404.track14.domain.rules.NewAppField
import uz.beko404.track14.domain.rules.NewAppRules
import uz.beko404.track14.domain.rules.ScoringRules
import uz.beko404.track14.domain.time.Clock
import javax.inject.Inject

/** Shared helper: the owner's apps for whoever is signed in; null = signed out. */
@OptIn(ExperimentalCoroutinesApi::class)
private fun myApps(auth: AuthRepository, owner: OwnerRepository): Flow<DomainResult<List<OwnerApp>>?> =
    auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn) owner.observeMyApps(s.uid) else flowOf(null)
    }

// ---------------------------------------------------------------- list

sealed interface MyAppsUiState {
    data object Loading : MyAppsUiState
    data object SignedOut : MyAppsUiState
    data class Error(val error: DomainError) : MyAppsUiState
    data class Content(val apps: List<OwnerApp>) : MyAppsUiState
}

@HiltViewModel
class MyAppsViewModel @Inject constructor(auth: AuthRepository, owner: OwnerRepository) : ViewModel() {
    val state: StateFlow<MyAppsUiState> = myApps(auth, owner).map { r ->
        when (r) {
            null -> MyAppsUiState.SignedOut
            is DomainResult.Failure -> MyAppsUiState.Error(r.error)
            is DomainResult.Success -> MyAppsUiState.Content(r.value)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MyAppsUiState.Loading)
}

// ---------------------------------------------------------------- add app

data class AddAppUiState(
    val form: NewApp = NewApp("", "", "", "", ""),
    /** Opt-in link follows the package name until the owner edits it. */
    val optInEdited: Boolean = false,
    val showErrors: Boolean = false,
    val saving: Boolean = false,
    val error: DomainError? = null,
    val createdAppId: String? = null,
    val apk: ApkState = ApkState.None,
) {
    val errors: Set<NewAppField> get() = if (showErrors) NewAppRules.errors(form) else emptySet()
}

/** Prefill from an APK: reading, done (shows the file's package and version) or failed. */
sealed interface ApkState {
    data object None : ApkState
    data object Reading : ApkState
    data class Filled(val packageName: String, val versionName: String) : ApkState
    data object TooLarge : ApkState
    data object Unreadable : ApkState
}

@HiltViewModel
class AddAppViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val owner: OwnerRepository,
    private val apkReader: ApkReader,
) : ViewModel() {
    private val _state = MutableStateFlow(AddAppUiState())
    val state: StateFlow<AddAppUiState> = _state.asStateFlow()

    fun onName(v: String) = edit { it.copy(name = v.take(NewAppRules.MAX_NAME)) }
    fun onCategory(v: String) = edit { it.copy(category = v) }
    fun onDescription(v: String) = edit { it.copy(shortDesc = v.take(NewAppRules.MAX_DESC)) }

    fun onPackage(v: String) = _state.update { s ->
        val pkg = v.filter { it.isLetterOrDigit() || it == '.' || it == '_' }.take(150)
        val optIn = if (s.optInEdited) s.form.optInUrl else NewAppRules.defaultOptInUrl(pkg)
        s.copy(form = s.form.copy(packageName = pkg, optInUrl = optIn))
    }

    fun onOptIn(v: String) = _state.update { it.copy(form = it.form.copy(optInUrl = v.take(300)), optInEdited = true) }

    /** Fills name, package and version from the picked APK; fields stay editable afterwards. */
    fun onApkPicked(uri: Uri?) {
        if (uri == null || _state.value.apk == ApkState.Reading) return
        _state.update { it.copy(apk = ApkState.Reading) }
        viewModelScope.launch {
            when (val r = apkReader.read(uri)) {
                is ApkReadResult.Ok -> {
                    onPackage(r.info.packageName)
                    _state.update { s ->
                        s.copy(
                            form = s.form.copy(
                                name = r.info.name.ifBlank { s.form.name },
                                versionName = r.info.versionName,
                                fromApk = true,
                            ),
                            apk = ApkState.Filled(r.info.packageName, r.info.versionName),
                        )
                    }
                }
                ApkReadResult.TooLarge -> _state.update { it.copy(apk = ApkState.TooLarge) }
                ApkReadResult.Unreadable -> _state.update { it.copy(apk = ApkState.Unreadable) }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.saving) return
        if (NewAppRules.errors(s.form).isNotEmpty()) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val session = auth.session.first()
            val result = if (session is Session.SignedIn) {
                owner.createDraft(session.uid, s.form)
            } else {
                DomainResult.Failure(DomainError.Unauthenticated)
            }
            _state.update {
                when (result) {
                    is DomainResult.Success -> it.copy(saving = false, createdAppId = result.value)
                    is DomainResult.Failure -> it.copy(saving = false, error = result.error)
                }
            }
        }
    }

    private fun edit(change: (NewApp) -> NewApp) = _state.update { it.copy(form = change(it.form)) }
}

// ---------------------------------------------------------------- plan & payment

data class PlanUiState(
    val app: OwnerApp? = null,
    val economy: EconomyConfig = EconomyConfig(),
    val balance: Coins? = null,
    val selected: Plan = Plan.STANDARD,
    val paying: Boolean = false,
    val error: DomainError? = null,
    val paid: Boolean = false,
) {
    val fee: Coins get() = economy.fee(selected)
    val canPay: Boolean
        get() = app != null && balance != null && !paying && ScoringRules(economy).canAffordListing(balance, selected)
}

@HiltViewModel
class PlanViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    auth: AuthRepository,
    private val owner: OwnerRepository,
    economy: EconomyRepository,
    wallet: WalletRepository,
) : ViewModel() {
    private val appId: String? = savedStateHandle.get<String>(ARG_APP_ID)?.takeIf { AppIds.isValid(it) }
    private val local = MutableStateFlow(PlanUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val balance: Flow<Coins?> = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn) wallet.observeBalance(s.uid).map { (it as? DomainResult.Success)?.value } else flowOf(null)
    }

    val state: StateFlow<PlanUiState> = combine(myApps(auth, owner), economy.observe(), balance, local) { apps, eco, bal, l ->
        l.copy(
            app = (apps as? DomainResult.Success)?.value?.firstOrNull { it.id == appId },
            economy = eco,
            balance = bal,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun select(plan: Plan) = local.update { it.copy(selected = plan, error = null) }

    fun pay() {
        val id = appId ?: return
        if (!state.value.canPay) return
        val plan = state.value.selected
        local.update { it.copy(paying = true, error = null) }
        viewModelScope.launch {
            when (val r = owner.createCampaign(id, plan)) {
                is DomainResult.Success -> local.update { it.copy(paying = false, paid = true) }
                is DomainResult.Failure -> local.update { it.copy(paying = false, error = r.error) }
            }
        }
    }
}

// ---------------------------------------------------------------- dashboard

data class DashboardUiState(
    val loading: Boolean = true,
    val app: OwnerApp? = null,
    val testers: List<CampaignTester> = emptyList(),
    val feedback: List<Feedback> = emptyList(),
    val busy: Boolean = false,
    val error: DomainError? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val auth: AuthRepository,
    private val owner: OwnerRepository,
    feedbackRepository: FeedbackRepository,
    economy: EconomyRepository,
    private val clock: Clock,
) : ViewModel() {
    val economyConfig: StateFlow<EconomyConfig> = economy.observe()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EconomyConfig())

    fun now(): Long = clock.nowMillis()

    /** Top placement for 1, 3 or 7 days. A fresh request id per tap; the busy flag blocks double taps. */
    fun feature(days: Int) {
        val id = appId ?: return
        if (local.value.busy) return
        local.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val r = owner.featureApp(id, days, java.util.UUID.randomUUID().toString().replace("-", ""))
            local.update { it.copy(busy = false, error = (r as? DomainResult.Failure)?.error) }
        }
    }

    private val appId: String? = savedStateHandle.get<String>(ARG_APP_ID)?.takeIf { AppIds.isValid(it) }
    private val local = MutableStateFlow(DashboardUiState())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testers: Flow<List<CampaignTester>> = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn && appId != null) {
            owner.observeTesters(s.uid, appId).map { (it as? DomainResult.Success)?.value.orEmpty() }
        } else {
            flowOf(emptyList())
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val feedback: Flow<List<Feedback>> = auth.session.flatMapLatest { s ->
        if (s is Session.SignedIn && appId != null) {
            feedbackRepository.observeForApp(s.uid, appId).map { (it as? DomainResult.Success)?.value.orEmpty() }
        } else {
            flowOf(emptyList())
        }
    }

    val state: StateFlow<DashboardUiState> = combine(myApps(auth, owner), testers, feedback, local) { apps, list, fb, l ->
        l.copy(
            loading = false,
            app = (apps as? DomainResult.Success)?.value?.firstOrNull { it.id == appId },
            testers = list,
            feedback = fb,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun act(action: OwnerAction) {
        val id = appId ?: return
        if (local.value.busy) return
        local.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            val r = owner.setStatus(id, action)
            local.update { it.copy(busy = false, error = (r as? DomainResult.Failure)?.error) }
        }
    }
}

const val ARG_APP_ID = "appId"
