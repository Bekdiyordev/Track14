package uz.beko404.track14.data.social

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.beko404.track14.data.apps.toMillis
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.model.AppNotification
import uz.beko404.track14.domain.model.Feedback
import uz.beko404.track14.domain.model.FeedbackRules
import uz.beko404.track14.domain.model.FeedbackTag
import uz.beko404.track14.domain.model.NewFeedback
import uz.beko404.track14.domain.model.ReportReason
import uz.beko404.track14.domain.model.ReportTarget
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.FeedbackRepository
import uz.beko404.track14.domain.repository.NotificationsRepository
import uz.beko404.track14.domain.repository.ReportRepository
import javax.inject.Inject

private suspend fun write(block: suspend () -> Unit): DomainResult<Unit> = try {
    block()
    DomainResult.Success(Unit)
} catch (e: Exception) {
    DomainResult.Failure(e.toDomainError())
}

class FirestoreFeedbackRepository @Inject constructor(private val firestore: FirebaseFirestore) : FeedbackRepository {

    override suspend fun submit(uid: String, test: TesterTest, feedback: NewFeedback): DomainResult<Unit> {
        if (!FeedbackRules.isValid(feedback)) return DomainResult.Failure(DomainError.Rejected("invalid_input"))
        return write {
            firestore.collection("feedback").document().set(
                mapOf(
                    "testId" to test.id,
                    "appId" to test.appId,
                    "testerUid" to uid,
                    "ownerUid" to test.ownerUid,
                    "rating" to feedback.rating,
                    "tags" to feedback.tags.map { it.wireName },
                    "text" to feedback.text.trim(),
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    override fun observeForApp(ownerUid: String, appId: String): Flow<DomainResult<List<Feedback>>> = callbackFlow {
        val registration = firestore.collection("feedback")
            .whereEqualTo("appId", appId)
            .whereEqualTo("ownerUid", ownerUid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { feedbackFrom(d.id, it) } }))
                }
            }
        awaitClose { registration.remove() }
    }
}

class FirestoreReportRepository @Inject constructor(private val firestore: FirebaseFirestore) : ReportRepository {
    override suspend fun submit(
        uid: String,
        target: ReportTarget,
        targetId: String,
        reason: ReportReason,
        comment: String,
    ): DomainResult<Unit> {
        if (target !in reason.targets) return DomainResult.Failure(DomainError.Rejected("invalid_input"))
        return write {
            firestore.collection("reports").document().set(
                mapOf(
                    "reporterUid" to uid,
                    "targetType" to target.wireName,
                    "targetId" to targetId.take(160),
                    "reason" to reason.wireName,
                    "comment" to comment.trim().take(1_000),
                    "status" to "open",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }
}

class FirestoreNotificationsRepository @Inject constructor(private val firestore: FirebaseFirestore) : NotificationsRepository {
    private fun user(uid: String) = firestore.collection("users").document(uid)

    override fun observe(uid: String): Flow<DomainResult<List<AppNotification>>> = callbackFlow {
        val registration = user(uid).collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { notificationFrom(d.id, it) } }))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun markRead(uid: String, id: String) {
        runCatching { user(uid).collection("notifications").document(id).update("read", true).await() }
    }

    override suspend fun registerToken(uid: String, token: String) {
        runCatching {
            user(uid).collection("tokens").document(token)
                .set(mapOf("platform" to "android", "updatedAt" to FieldValue.serverTimestamp())).await()
        }
    }
}

internal fun feedbackFrom(id: String, data: Map<String, Any?>): Feedback? {
    val rating = (data["rating"] as? Number)?.toInt()?.takeIf { it in 1..5 } ?: return null
    return Feedback(
        id = id,
        rating = rating,
        tags = (data["tags"] as? List<*>).orEmpty().mapNotNull { FeedbackTag.fromWire(it as? String) }.toSet(),
        text = data["text"] as? String ?: "",
        createdAtMillis = data["createdAt"].toMillis(),
    )
}

internal fun notificationFrom(id: String, data: Map<String, Any?>): AppNotification? {
    val title = data["title"] as? String ?: return null
    return AppNotification(
        id = id,
        type = data["type"] as? String ?: "",
        title = title,
        body = data["body"] as? String ?: "",
        refId = data["refId"] as? String ?: "",
        read = data["read"] == true,
        createdAtMillis = data["createdAt"].toMillis(),
    )
}
