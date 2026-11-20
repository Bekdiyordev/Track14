package uz.beko404.track14.data.apps

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.Plan
import uz.beko404.track14.domain.model.PlayLinks
import uz.beko404.track14.domain.repository.AppsRepository
import uz.beko404.track14.domain.repository.CatalogApp
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import javax.inject.Inject

/**
 * Public catalog: `catalog/{appId}` is a denormalised projection written only by
 * Cloud Functions. It carries no owner data, so it is safe to read without login.
 * Filtering and sorting happen in [uz.beko404.track14.domain.rules.CatalogRules];
 * server-side paging replaces the fixed page size when the catalog grows.
 */
class FirestoreAppsRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : AppsRepository {

    override fun observeCatalog(): Flow<DomainResult<List<CatalogApp>>> = callbackFlow {
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo("status", "active")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(CATALOG_PAGE_SIZE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    val apps = snapshot?.documents.orEmpty()
                        .mapNotNull { doc -> doc.data?.let { catalogAppFrom(doc.id, it) } }
                    trySend(DomainResult.Success(apps))
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeApp(id: String): Flow<DomainResult<CatalogApp>> = callbackFlow {
        val registration = firestore.collection(COLLECTION).document(id)
            .addSnapshotListener { snapshot, error ->
                val result = when {
                    error != null -> DomainResult.Failure(error.toDomainError())
                    snapshot == null || !snapshot.exists() -> DomainResult.Failure(DomainError.NotFound)
                    else -> snapshot.data?.let { catalogAppFrom(snapshot.id, it) }
                        ?.let { DomainResult.Success(it) }
                        ?: DomainResult.Failure(DomainError.NotFound)
                }
                trySend(result)
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val COLLECTION = "catalog"
        const val CATALOG_PAGE_SIZE = 50L
    }
}

private const val DEFAULT_TESTERS_NEEDED = 12
private const val MAX_NAME = 60
private const val MAX_DESC = 300

/** Returns null for documents without a name, so one bad document never breaks the list. */
internal fun catalogAppFrom(id: String, data: Map<String, Any?>): CatalogApp? {
    val name = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return CatalogApp(
        id = id,
        name = name.take(MAX_NAME),
        category = (data["category"] as? String)?.trim().orEmpty(),
        testersJoined = (data["joined"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
        testersNeeded = (data["needed"] as? Number)?.toInt()?.takeIf { it > 0 } ?: DEFAULT_TESTERS_NEEDED,
        rewardPerDay = Coins((data["rewardPerDay"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L),
        isPro = Plan.fromWire(data["plan"] as? String) == Plan.PRO,
        shortDesc = (data["shortDesc"] as? String)?.trim()?.take(MAX_DESC).orEmpty(),
        rating = (data["rating"] as? Number)?.toDouble()?.takeIf { it in 0.0..5.0 },
        createdAtMillis = data["createdAt"].toMillis(),
        featuredUntilMillis = data["featuredUntil"].toMillis(),
        packageName = (data["packageName"] as? String)?.takeIf(PlayLinks::isValidPackageName).orEmpty(),
        optInUrl = (data["optInUrl"] as? String)?.takeIf(PlayLinks::isValidOptInUrl).orEmpty(),
        completionBonus = Coins((data["completionBonus"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L),
        ownerName = (data["ownerName"] as? String)?.trim()?.take(40).orEmpty(),
        ownerReliability = (data["ownerReliability"] as? Number)?.toInt()?.coerceIn(0, 100),
        ownerTests = (data["ownerTests"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0,
    )
}

internal fun Any?.toMillis(): Long = when (this) {
    is Timestamp -> toDate().time
    is Number -> toLong()
    else -> 0L
}
