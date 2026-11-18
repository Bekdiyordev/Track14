package uz.beko404.track14.admin.data

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import uz.beko404.track14.admin.logic.Role
import uz.beko404.track14.domain.model.AppStatus
import uz.beko404.track14.domain.model.TesterStats
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Everything the admin app reads and does. Reads are plain Firestore queries allowed by the
 * staff rules; every change is a callable admin* Function (role check + audit log on the server).
 */
@Singleton
class AdminRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) {
    // ------------------------------------------------------------ session

    /** Role from a fresh ID token, so a just-granted role is picked up. */
    suspend fun currentRole(): Role? {
        val user = auth.currentUser ?: return null
        return runCatching { Role.fromClaim(user.getIdToken(true).await().claims["role"]) }.getOrNull()
    }

    fun isSignedIn(): Boolean = auth.currentUser != null

    fun signOut() = auth.signOut()

    // ------------------------------------------------------------ reads

    suspend fun dashboard(): DomainResult<DashboardCounts> = load {
        coroutineScope {
            val users = async { count(db.collection("users")) }
            val active = async { count(db.collection("campaigns").whereEqualTo("status", "active")) }
            val reports = async { count(db.collection("reports").whereEqualTo("status", "open")) }
            val pushes = async { count(db.collection("pushJobs").whereEqualTo("status", "scheduled")) }
            DashboardCounts(users.await(), active.await(), reports.await(), pushes.await())
        }
    }

    /** Exact e-mail or uid search; empty query = newest users. */
    suspend fun users(query: String): DomainResult<List<AdminUser>> = load {
        val q = query.trim()
        val docs = when {
            q.contains('@') -> db.collection("users").whereEqualTo("email", q.lowercase()).limit(20).get().await().documents
            q.isNotEmpty() -> listOfNotNull(db.collection("users").document(q).get().await().takeIf { it.exists() })
            else -> db.collection("users").orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().await().documents
        }
        docs.mapNotNull(::userFrom)
    }

    suspend fun user(uid: String): DomainResult<UserDetail> = load {
        coroutineScope {
            val doc = async { db.collection("users").document(uid).get().await() }
            val wallet = async { db.collection("wallets").document(uid).get().await() }
            val ledger = async {
                db.collection("transactions").whereEqualTo("uid", uid)
                    .orderBy("createdAt", Query.Direction.DESCENDING).limit(30).get().await().documents
            }
            val user = userFrom(doc.await()) ?: throw NotFound
            UserDetail(
                user = user,
                balance = (wallet.await().get("balance") as? Number)?.toLong() ?: 0L,
                ledger = ledger.await().map {
                    LedgerRow(it.id, it.getString("type").orEmpty(), (it.get("amount") as? Number)?.toLong() ?: 0, it.millis("createdAt"))
                },
            )
        }
    }

    suspend fun apps(status: AppStatus?): DomainResult<List<AdminApp>> = load {
        var q: Query = db.collection("apps")
        if (status != null) q = q.whereEqualTo("status", status.wireName)
        q.orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get().await().documents.mapNotNull(::appFrom)
    }

    suspend fun app(appId: String): DomainResult<AppDetailAdmin> = load {
        coroutineScope {
            val app = async { db.collection("apps").document(appId).get().await() }
            val campaign = async { db.collection("campaigns").document(appId).get().await() }
            val a = app.await()
            val c = campaign.await()
            val stats = c.get("stats") as? Map<*, *>
            fun n(key: String) = (stats?.get(key) as? Number)?.toInt() ?: 0
            AppDetailAdmin(
                app = appFrom(a) ?: throw NotFound,
                campaignStatus = c.getString("status"),
                plan = c.getString("plan"),
                feePaid = (c.get("feePaid") as? Number)?.toLong() ?: 0L,
                joined = n("joined"),
                completed = n("completed14"),
                failed = n("failed") + n("left"),
                suspendedReason = a.getString("suspendedReason").orEmpty(),
            )
        }
    }

    suspend fun reports(status: String): DomainResult<List<Report>> = load {
        db.collection("reports").whereEqualTo("status", status)
            .orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get().await().documents.map {
                Report(
                    id = it.id,
                    targetType = it.getString("targetType").orEmpty(),
                    targetId = it.getString("targetId").orEmpty(),
                    reason = it.getString("reason").orEmpty(),
                    comment = it.getString("comment").orEmpty(),
                    reporterUid = it.getString("reporterUid").orEmpty(),
                    status = it.getString("status").orEmpty(),
                    createdAtMillis = it.millis("createdAt"),
                )
            }
    }

    suspend fun pushJobs(): DomainResult<List<PushJob>> = load {
        db.collection("pushJobs").orderBy("createdAt", Query.Direction.DESCENDING).limit(50).get().await().documents.map {
            val target = it.get("target") as? Map<*, *>
            PushJob(
                id = it.id,
                title = it.getString("title").orEmpty(),
                body = it.getString("body").orEmpty(),
                target = if (target?.get("kind") == "all") "all" else "${(target?.get("uids") as? List<*>)?.size ?: 0} users",
                status = it.getString("status").orEmpty(),
                sendAtMillis = it.millis("sendAt"),
            )
        }
    }

    suspend fun audit(): DomainResult<List<AuditEntry>> = load {
        db.collection("auditLogs").orderBy("createdAt", Query.Direction.DESCENDING).limit(100).get().await().documents.map {
            AuditEntry(it.id, it.getString("actor").orEmpty(), it.getString("action").orEmpty(), it.getString("target").orEmpty(), it.millis("createdAt"))
        }
    }

    /** Raw config document (`app_version`, `support`, `flags`, `economy`). */
    suspend fun config(doc: String): DomainResult<Map<String, Any?>> = load {
        db.collection("config").document(doc).get().await().data.orEmpty() - "updatedAt"
    }

    // ------------------------------------------------------------ actions (callables)

    suspend fun setRole(uid: String, role: Role?) = call("adminSetRole", mapOf("uid" to uid, "role" to role?.claim))
    suspend fun blockUser(uid: String, blocked: Boolean, reason: String) =
        call("adminBlockUser", mapOf("uid" to uid, "blocked" to blocked, "reason" to reason.trim()))
    suspend fun adjustCoins(uid: String, amount: Int, reason: String) =
        call("adminAdjustCoins", mapOf("uid" to uid, "amount" to amount, "reason" to reason.trim()))
    suspend fun suspendApp(appId: String, suspend: Boolean, reason: String, refundFee: Boolean) =
        call("adminSuspendApp", mapOf("appId" to appId, "suspend" to suspend, "reason" to reason.trim(), "refundFee" to refundFee))
    suspend fun resolveReport(id: String, status: String, note: String) =
        call("adminResolveReport", mapOf("reportId" to id, "status" to status, "note" to note.trim()))
    suspend fun setConfig(doc: String, value: Map<String, Any>) = call("adminSetConfig", mapOf("doc" to doc, "value" to value))
    suspend fun cancelPush(jobId: String) = call("adminCancelPush", mapOf("jobId" to jobId))

    suspend fun sendPush(uids: List<String>?, title: String, body: String, sendAtMillis: Long?): DomainResult<Unit> {
        val target: Map<String, Any> = if (uids == null) mapOf("kind" to "all") else mapOf("kind" to "users", "uids" to uids)
        val input = buildMap<String, Any> {
            put("target", target)
            put("title", title.trim())
            put("body", body.trim())
            sendAtMillis?.let { put("sendAt", it) }
        }
        return call("adminSendPush", input)
    }

    // ------------------------------------------------------------ helpers

    private suspend fun call(name: String, input: Map<String, Any?>): DomainResult<Unit> = load {
        functions.getHttpsCallable(name).call(input.filterValues { it != null }).await()
        Unit
    }

    private suspend fun count(query: Query): Long = query.count().get(AggregateSource.SERVER).await().count

    private object NotFound : RuntimeException()

    private suspend fun <T> load(block: suspend () -> T): DomainResult<T> = try {
        DomainResult.Success(block())
    } catch (e: NotFound) {
        DomainResult.Failure(DomainError.NotFound)
    } catch (e: com.google.firebase.functions.FirebaseFunctionsException) {
        DomainResult.Failure(
            when (e.code) {
                com.google.firebase.functions.FirebaseFunctionsException.Code.PERMISSION_DENIED -> DomainError.PermissionDenied
                com.google.firebase.functions.FirebaseFunctionsException.Code.UNAVAILABLE -> DomainError.Network
                else -> DomainError.Rejected(e.message.orEmpty().take(64))
            },
        )
    } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
        DomainResult.Failure(
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) DomainError.PermissionDenied else DomainError.Network,
        )
    } catch (e: Exception) {
        DomainResult.Failure(DomainError.Unknown())
    }
}

private fun DocumentSnapshot.millis(field: String): Long = (get(field) as? Timestamp)?.toDate()?.time ?: 0L

private fun userFrom(d: DocumentSnapshot): AdminUser? {
    if (!d.exists()) return null
    val stats = d.get("stats") as? Map<*, *>
    fun n(key: String) = ((stats?.get(key) as? Number)?.toInt() ?: 0).coerceAtLeast(0)
    return AdminUser(
        uid = d.id,
        name = d.getString("displayName").orEmpty(),
        email = d.getString("email").orEmpty(),
        blocked = d.getBoolean("blocked") == true,
        blockedReason = d.getString("blockedReason").orEmpty(),
        lang = d.getString("lang").orEmpty(),
        stats = TesterStats(n("testsStarted"), n("testsCompleted"), n("testsFailed"), n("testsLeft"), n("daysPassed")),
        createdAtMillis = d.millis("createdAt"),
    )
}

private fun appFrom(d: DocumentSnapshot): AdminApp? {
    if (!d.exists()) return null
    return AdminApp(
        id = d.id,
        name = d.getString("name").orEmpty(),
        packageName = d.getString("packageName").orEmpty(),
        ownerUid = d.getString("ownerUid").orEmpty(),
        status = AppStatus.fromWire(d.getString("status")),
        category = d.getString("category").orEmpty(),
        createdAtMillis = d.millis("createdAt"),
    )
}
