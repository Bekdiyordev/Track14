package uz.beko404.track14.data.published

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.beko404.track14.data.apps.toMillis
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.model.PlayLinks
import uz.beko404.track14.domain.model.PublishedApp
import uz.beko404.track14.domain.model.PublishedTotals
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.PublishedRepository
import javax.inject.Inject

/** Public `published/{appId}` and `stats/published`, both written by Functions on "Published". */
class FirestorePublishedRepository @Inject constructor(private val firestore: FirebaseFirestore) : PublishedRepository {

    override fun observeApps(): Flow<DomainResult<List<PublishedApp>>> = callbackFlow {
        val registration = firestore.collection("published")
            .orderBy("publishedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { publishedFrom(d.id, it) } }))
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeTotals(): Flow<PublishedTotals> = callbackFlow {
        val registration = firestore.document("stats/published").addSnapshotListener { snapshot, _ ->
            trySend(
                PublishedTotals(
                    count = (snapshot?.get("count") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                    testers = (snapshot?.get("testers") as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
                ),
            )
        }
        awaitClose { registration.remove() }
    }
}

internal fun publishedFrom(id: String, data: Map<String, Any?>): PublishedApp? {
    val name = (data["name"] as? String)?.takeIf { it.isNotBlank() } ?: return null
    fun n(key: String) = ((data[key] as? Number)?.toInt() ?: 0).coerceAtLeast(0)
    return PublishedApp(
        id = id,
        name = name,
        category = data["category"] as? String ?: "",
        packageName = (data["packageName"] as? String)?.takeIf { PlayLinks.isValidPackageName(it) }.orEmpty(),
        testers = n("testers"),
        completed = n("completed"),
        feedbackCount = n("feedbackCount"),
        rating = (data["rating"] as? Number)?.toDouble()?.takeIf { it in 0.0..5.0 },
        publishedAtMillis = data["publishedAt"].toMillis(),
    )
}
