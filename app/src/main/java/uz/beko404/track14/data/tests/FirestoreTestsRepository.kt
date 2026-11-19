package uz.beko404.track14.data.tests

import com.google.firebase.Timestamp
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
import uz.beko404.track14.data.callFunction
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.DayRecord
import uz.beko404.track14.domain.model.DayStatus
import uz.beko404.track14.domain.model.PlayLinks
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.DayCheckResult
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.TestsRepository
import uz.beko404.track14.domain.rules.CheckMode
import uz.beko404.track14.domain.rules.StreakRules
import javax.inject.Inject

/** Reads `tests` (rules: only the tester and the owner can read) and calls the test Functions. */
class FirestoreTestsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : TestsRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMyTests(uid: String): Flow<DomainResult<List<TesterTest>>> =
        testDocs(uid).flatMapLatest { result ->
            when (result) {
                is DomainResult.Failure -> flowOf(result)
                is DomainResult.Success ->
                    if (result.value.isEmpty()) {
                        flowOf(DomainResult.Success(emptyList()))
                    } else {
                        val perTest = result.value.map { (id, data) -> observeDays(firestore, id).map { days -> testerTestFrom(id, data, days) } }
                        combine(perTest) { tests ->
                            DomainResult.Success(
                                tests.filterNotNull().sortedByDescending { it.startedAtMillis ?: Long.MAX_VALUE },
                            )
                        }
                    }
            }
        }

    private fun testDocs(uid: String): Flow<DomainResult<List<Pair<String, Map<String, Any?>>>>> = callbackFlow {
        val registration = firestore.collection("tests")
            .whereEqualTo("testerUid", uid)
            .limit(MAX_TESTS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { d.id to it } }))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun startTest(campaignId: String, timezone: String): DomainResult<String> =
        functions.callFunction("startTest", mapOf("campaignId" to campaignId, "timezone" to timezone)) { data ->
            data["testId"] as? String
        }

    override suspend fun verifyDay(testId: String, seconds: Int, mode: CheckMode): DomainResult<DayCheckResult> =
        functions.callFunction("verifyDay", mapOf("testId" to testId, "seconds" to seconds, "mode" to mode.wireName)) { data ->
            DayCheckResult(
                day = (data["day"] as? Number)?.toInt() ?: 0,
                passed = data["passed"] == true,
                completed = data["completed"] == true,
            )
        }

    override suspend fun leaveTest(testId: String): DomainResult<Unit> =
        functions.callFunction("leaveTest", mapOf("testId" to testId)) { }

    override fun observeDayRecords(testId: String): Flow<List<DayRecord>> = callbackFlow {
        val registration = firestore.collection("tests").document(testId).collection("days")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { dayRecordFrom(d.id, it) } })
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val MAX_TESTS = 50L
    }
}

/** Day cells of one test (`tests/{id}/days`); a read error is treated as "no days yet". */
internal fun observeDays(firestore: FirebaseFirestore, testId: String): Flow<Map<Int, DayStatus>> = callbackFlow {
    val registration = firestore.collection("tests").document(testId).collection("days")
        .addSnapshotListener { snapshot, _ ->
            val days = snapshot?.documents.orEmpty().mapNotNull { d ->
                val n = d.id.toIntOrNull() ?: return@mapNotNull null
                n to dayStatusFrom(d.getString("status"))
            }.toMap()
            trySend(days)
        }
    awaitClose { registration.remove() }
}

/** Null for ids that are not day numbers 1..14. Seconds and mode are whatever the server stored. */
internal fun dayRecordFrom(id: String, data: Map<String, Any?>): DayRecord? {
    val n = id.toIntOrNull()?.takeIf { it in 1..StreakRules.TEST_DAYS } ?: return null
    return DayRecord(
        day = n,
        status = dayStatusFrom(data["status"] as? String),
        seconds = (data["seconds"] as? Number)?.toInt()?.coerceIn(0, 86_400),
        mode = CheckMode.entries.firstOrNull { it.wireName == data["mode"] },
        verifiedAtMillis = (data["verifiedAt"] as? Timestamp)?.toDate()?.time,
    )
}

internal fun dayStatusFrom(value: String?): DayStatus = when (value) {
    "passed" -> DayStatus.PASSED
    "missed" -> DayStatus.MISSED
    else -> DayStatus.PENDING
}

internal fun testStatusFrom(value: String?): TestStatus? = when (value) {
    "joining" -> TestStatus.JOINING
    "active" -> TestStatus.ACTIVE
    "completed" -> TestStatus.COMPLETED
    "failed" -> TestStatus.FAILED
    "left" -> TestStatus.LEFT
    else -> null
}

/** Null for unreadable documents, so one bad test never hides the others. */
internal fun testerTestFrom(id: String, data: Map<String, Any?>, days: Map<Int, DayStatus>): TesterTest? {
    val status = testStatusFrom(data["status"] as? String) ?: return null
    val startedAt = when (val v = data["startedAt"]) {
        is Timestamp -> v.toDate().time
        is Number -> v.toLong()
        else -> null
    }
    return TesterTest(
        id = id,
        campaignId = data["campaignId"] as? String ?: return null,
        appId = data["appId"] as? String ?: "",
        appName = (data["appName"] as? String)?.takeIf { it.isNotBlank() } ?: "—",
        packageName = (data["packageName"] as? String)?.takeIf(PlayLinks::isValidPackageName).orEmpty(),
        status = status,
        rewardPerDay = Coins((data["rewardPerDay"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L),
        timezone = data["timezone"] as? String ?: "UTC",
        startedAtMillis = startedAt,
        days = List(StreakRules.TEST_DAYS) { i -> days[i + 1] ?: DayStatus.PENDING },
        ownerUid = data["ownerUid"] as? String ?: "",
        feedbackRewarded = data["feedbackRewarded"] == true,
        completionBonus = Coins((data["completionBonus"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L),
    )
}
