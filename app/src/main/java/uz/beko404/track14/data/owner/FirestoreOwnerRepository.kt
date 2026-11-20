package uz.beko404.track14.data.owner

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import uz.beko404.track14.data.apps.toMillis
import uz.beko404.track14.data.callFunction
import uz.beko404.track14.data.tests.observeDays
import uz.beko404.track14.data.tests.testStatusFrom
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.CampaignSummary
import uz.beko404.track14.domain.model.CampaignTester
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.OwnerAction
import uz.beko404.track14.domain.model.OwnerApp
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.OwnerRepository
import uz.beko404.track14.domain.rules.NewApp
import uz.beko404.track14.domain.rules.NewAppRules
import uz.beko404.track14.domain.rules.StreakRules
import javax.inject.Inject

private typealias Docs = DomainResult<List<Pair<String, Map<String, Any?>>>>

class FirestoreOwnerRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : OwnerRepository {

    override fun observeMyApps(uid: String): Flow<DomainResult<List<OwnerApp>>> =
        combine(ownedDocs("apps", uid), ownedDocs("campaigns", uid)) { apps, campaigns ->
            when {
                apps is DomainResult.Failure -> apps
                campaigns is DomainResult.Failure -> campaigns
                else -> {
                    val byId = (campaigns as DomainResult.Success).value.toMap()
                    val list = (apps as DomainResult.Success).value
                        .mapNotNull { (id, data) -> ownerAppFrom(id, data, byId[id]) }
                        .filter { it.status != AppStatus.REMOVED }
                        .sortedWith(compareBy<OwnerApp> { it.status.ordinal }.thenBy { it.name })
                    DomainResult.Success(list)
                }
            }
        }

    private fun ownedDocs(collection: String, uid: String): Flow<Docs> = callbackFlow {
        val registration = firestore.collection(collection)
            .whereEqualTo("ownerUid", uid)
            .limit(MAX_APPS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { d.id to it } }))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun createDraft(uid: String, app: NewApp): DomainResult<String> = try {
        val clean = NewAppRules.normalize(app)
        val ref = firestore.collection("apps").document()
        ref.set(
            buildMap {
                put("ownerUid", uid)
                put("status", AppStatus.DRAFT.wireName)
                put("createdAt", FieldValue.serverTimestamp())
                put("name", clean.name)
                put("packageName", clean.packageName)
                put("category", clean.category)
                put("shortDesc", clean.shortDesc)
                put("optInUrl", clean.optInUrl)
                put("source", if (clean.fromApk) "apk" else "manual")
                if (clean.versionName.isNotBlank()) put("versionName", clean.versionName.take(40))
            },
        ).await()
        DomainResult.Success(ref.id)
    } catch (e: Exception) {
        DomainResult.Failure(e.toDomainError())
    }

    override suspend fun createCampaign(appId: String, plan: Plan): DomainResult<Unit> =
        functions.callFunction("createCampaign", mapOf("appId" to appId, "plan" to plan.wireName)) { }

    override suspend fun setStatus(appId: String, action: OwnerAction): DomainResult<Unit> =
        functions.callFunction("ownerSetStatus", mapOf("appId" to appId, "action" to action.wireName)) { }

    override suspend fun featureApp(appId: String, days: Int, requestId: String): DomainResult<Unit> =
        functions.callFunction("featureApp", mapOf("appId" to appId, "days" to days, "requestId" to requestId)) { }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTesters(uid: String, campaignId: String): Flow<DomainResult<List<CampaignTester>>> =
        testsOf(uid, campaignId).flatMapLatest { result ->
            when (result) {
                is DomainResult.Failure -> flowOf(result)
                is DomainResult.Success ->
                    if (result.value.isEmpty()) {
                        flowOf(DomainResult.Success(emptyList()))
                    } else {
                        val perTester = result.value.map { (id, data) -> observeDays(firestore, id).map { days -> campaignTesterFrom(id, data, days) } }
                        combine(perTester) { testers ->
                            DomainResult.Success(testers.filterNotNull().sortedBy { it.startedAtMillis ?: Long.MAX_VALUE })
                        }
                    }
            }
        }

    /** The rules let an owner read tests where ownerUid is theirs, so the query must say so. */
    private fun testsOf(uid: String, campaignId: String): Flow<Docs> = callbackFlow {
        val registration = firestore.collection("tests")
            .whereEqualTo("campaignId", campaignId)
            .whereEqualTo("ownerUid", uid)
            .limit(MAX_TESTERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { d.id to it } }))
                }
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val MAX_APPS = 50L
        const val MAX_TESTERS = 100L
    }
}

internal fun ownerAppFrom(id: String, data: Map<String, Any?>, campaign: Map<String, Any?>?): OwnerApp? {
    val status = AppStatus.fromWire(data["status"] as? String) ?: return null
    return OwnerApp(
        id = id,
        name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null,
        packageName = data["packageName"] as? String ?: "",
        category = data["category"] as? String ?: "",
        shortDesc = data["shortDesc"] as? String ?: "",
        optInUrl = data["optInUrl"] as? String ?: "",
        status = status,
        campaign = campaign?.let(::campaignSummaryFrom),
        suspendedReason = (data["suspendedReason"] as? String).orEmpty().take(300),
        updatedAtMillis = data["updatedAt"].toMillis(),
    )
}

internal fun campaignSummaryFrom(data: Map<String, Any?>): CampaignSummary? {
    val stats = data["stats"] as? Map<*, *> ?: emptyMap<String, Any>()
    fun count(key: String) = ((stats[key] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
    return CampaignSummary(
        plan = Plan.fromWire(data["plan"] as? String) ?: Plan.STANDARD,
        status = AppStatus.fromWire(data["status"] as? String) ?: return null,
        joined = count("joined"),
        needed = (data["testersNeeded"] as? Number)?.toInt()?.takeIf { it > 0 } ?: 12,
        completed = count("completed14"),
        failed = count("failed"),
        left = count("left"),
        startedAtMillis = data["startedAt"].toMillis().takeIf { it > 0 },
        featuredUntilMillis = data["featuredUntil"].toMillis(),
    )
}

internal fun campaignTesterFrom(id: String, data: Map<String, Any?>, days: Map<Int, DayStatus>): CampaignTester? {
    val status = testStatusFrom(data["status"] as? String) ?: return null
    return CampaignTester(
        testId = id,
        status = status,
        days = List(StreakRules.TEST_DAYS) { i -> days[i + 1] ?: DayStatus.PENDING },
        startedAtMillis = data["startedAt"].toMillis().takeIf { it > 0 },
        name = (data["testerName"] as? String).orEmpty().take(40),
        device = (data["testerDevice"] as? String).orEmpty().take(60),
        reliability = (data["testerReliability"] as? Number)?.toInt()?.coerceIn(0, 100),
        completedTests = (data["testerCompleted"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
        timezone = data["timezone"] as? String ?: "UTC",
    )
}
