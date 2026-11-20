package uz.beko404.track14.domain.repository

import kotlinx.coroutines.flow.Flow
import uz.beko404.track14.domain.model.AppNotification
import uz.beko404.track14.domain.model.CampaignTester
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.NewFeedback
import uz.beko404.track14.domain.model.ReportReason
import uz.beko404.track14.domain.model.ReportTarget
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.DayRecord
import uz.beko404.track14.domain.model.EconomyConfig
import uz.beko404.track14.domain.model.FeatureFlags
import uz.beko404.track14.domain.model.SupportConfig
import uz.beko404.track14.domain.model.VersionConfig
import uz.beko404.track14.domain.model.OwnerAction
import uz.beko404.track14.domain.model.OwnerApp
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.PublishedApp
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.model.TesterStats
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.model.WalletTransaction
import uz.beko404.track14.domain.rules.CheckMode
import uz.beko404.track14.domain.rules.NewApp

/** Signed-in state. Browsing apps needs no login, so SignedOut is a normal state. */
sealed interface Session {
    data object SignedOut : Session
    data class SignedIn(
        val uid: String,
        val displayName: String?,
        val email: String?,
        val photoUrl: String?,
    ) : Session
}

interface AuthRepository {
    val session: Flow<Session>

    /** Exchanges a Google ID token for a Firebase session. */
    suspend fun signInWithGoogle(idToken: String): DomainResult<Session.SignedIn>

    suspend fun signOut()

    /** Deletes the account and its data on the server (Play requirement), then signs out. */
    suspend fun deleteAccount(): DomainResult<Unit>
}

/** Creates the `users/{uid}` profile on first sign-in. Safe to call on every sign-in. */
interface ProfileRepository {
    suspend fun ensureProfile(user: Session.SignedIn): DomainResult<Unit>

    /** Language of push and inbox texts (users/{uid}.lang). */
    suspend fun updateLanguage(uid: String, languageTag: String)

    fun observeStats(uid: String): Flow<TesterStats>
}

interface PublishedRepository {
    fun observeApps(): Flow<DomainResult<List<PublishedApp>>>
    fun observeTotals(): Flow<PublishedTotals>
}

/** Balance is read-only on the client: only Cloud Functions write wallets. */
interface WalletRepository {
    fun observeBalance(uid: String): Flow<DomainResult<Coins>>

    /** Newest first. */
    fun observeTransactions(uid: String): Flow<DomainResult<List<WalletTransaction>>>

    /** Asks the server to check a Play purchase and credit the pack (idempotent). */
    suspend fun verifyPurchase(productId: String, purchaseToken: String): DomainResult<PurchaseResult>
}

enum class PurchaseResult { CREDITED, PENDING }

/** Public catalog entry of an app that is waiting for testers. Times are epoch millis. */
data class CatalogApp(
    val id: String,
    val name: String,
    val category: String,
    val testersJoined: Int,
    val testersNeeded: Int,
    val rewardPerDay: Coins,
    val isPro: Boolean,
    val shortDesc: String = "",
    val rating: Double? = null,
    val createdAtMillis: Long = 0L,
    val featuredUntilMillis: Long = 0L,
    /** Empty when the owner gave no valid value; checked with [uz.beko404.track14.domain.model.PlayLinks]. */
    val packageName: String = "",
    val optInUrl: String = "",
    val completionBonus: Coins = Coins(0),
    /** Owner card on the app page: short name and record from the campaign snapshot. */
    val ownerName: String = "",
    val ownerReliability: Int? = null,
    val ownerTests: Int = 0,
) {
    /** 0..1, how far the app is from the testers it needs. */
    val testersProgress: Float
        get() = if (testersNeeded <= 0) 1f else (testersJoined.toFloat() / testersNeeded).coerceIn(0f, 1f)
}

interface AppsRepository {
    fun observeCatalog(): Flow<DomainResult<List<CatalogApp>>>

    /** Emits [DomainError.NotFound] when the app is not (or no longer) in the catalog. */
    fun observeApp(id: String): Flow<DomainResult<CatalogApp>>
}

data class DayCheckResult(val day: Int, val passed: Boolean, val completed: Boolean)

/** Tests are created and changed only by Cloud Functions; these calls ask them to act. */
interface TestsRepository {
    fun observeMyTests(uid: String): Flow<DomainResult<List<TesterTest>>>

    /** Returns the test id. Joining the same campaign twice returns the same test. */
    suspend fun startTest(campaignId: String, timezone: String): DomainResult<String>

    suspend fun verifyDay(testId: String, seconds: Int, mode: CheckMode): DomainResult<DayCheckResult>

    suspend fun leaveTest(testId: String): DomainResult<Unit>

    /** Day documents of one test; readable by its tester and its owner. */
    fun observeDayRecords(testId: String): Flow<List<DayRecord>>
}

/** Owner side. Drafts are written by the client; everything after that by Functions. */
interface OwnerRepository {
    fun observeMyApps(uid: String): Flow<DomainResult<List<OwnerApp>>>

    /** Creates `apps/{id}` with status draft. Input must pass NewAppRules first. */
    suspend fun createDraft(uid: String, app: NewApp): DomainResult<String>

    /** Pays the listing fee and starts the campaign. */
    suspend fun createCampaign(appId: String, plan: Plan): DomainResult<Unit>

    suspend fun setStatus(appId: String, action: OwnerAction): DomainResult<Unit>

    /** Pays for top placement; [requestId] makes a retried call safe. */
    suspend fun featureApp(appId: String, days: Int, requestId: String): DomainResult<Unit>

    fun observeTesters(uid: String, campaignId: String): Flow<DomainResult<List<CampaignTester>>>
}

/** Public `config/economy`, falling back to the defaults. */
interface EconomyRepository {
    fun observe(): Flow<EconomyConfig>
}

interface FeedbackRepository {
    /** The +2 reward is paid by a Function for the first feedback of each test. */
    suspend fun submit(uid: String, test: TesterTest, feedback: NewFeedback): DomainResult<Unit>

    fun observeForApp(ownerUid: String, appId: String): Flow<DomainResult<List<Feedback>>>
}

interface ReportRepository {
    suspend fun submit(uid: String, target: ReportTarget, targetId: String, reason: ReportReason, comment: String): DomainResult<Unit>
}

interface NotificationsRepository {
    fun observe(uid: String): Flow<DomainResult<List<AppNotification>>>

    suspend fun markRead(uid: String, id: String)

    suspend fun registerToken(uid: String, token: String)
}

/** Public config documents; a missing or unreadable document gives the defaults. */
interface ConfigRepository {
    fun version(): Flow<VersionConfig>
    fun support(): Flow<SupportConfig>
    fun flags(): Flow<FeatureFlags>

    /** Reason when `users/{uid}.blocked` is true, otherwise null. */
    fun blockedReason(uid: String): Flow<String?>
}
