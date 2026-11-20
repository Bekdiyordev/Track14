package uz.beko404.track14.data.profile

import android.os.Build
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import uz.beko404.track14.domain.model.TesterStats
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.ProfileRepository
import uz.beko404.track14.domain.repository.Session
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

/**
 * Creates `users/{uid}` once. Only profile fields are written; reliability, stats and
 * `blocked` belong to Cloud Functions (see firestore.rules).
 */
class FirestoreProfileRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ProfileRepository {

    override suspend fun ensureProfile(user: Session.SignedIn): DomainResult<Unit> =
        try {
            val ref = firestore.collection("users").document(user.uid)
            if (!ref.get().await().exists()) {
                ref.set(newProfile(user)).await()
            }
            DomainResult.Success(Unit)
        } catch (e: Exception) {
            DomainResult.Failure(e.toDomainError())
        }

    override suspend fun updateLanguage(uid: String, languageTag: String) {
        runCatching { firestore.collection("users").document(uid).update("lang", languageTag).await() }
    }

    override suspend fun updatePushPrefs(uid: String, reminders: Boolean, owner: Boolean) {
        runCatching {
            firestore.collection("users").document(uid)
                .update(mapOf("notifyReminders" to reminders, "notifyOwner" to owner)).await()
        }
    }

    override fun observeStats(uid: String): Flow<TesterStats> = callbackFlow {
        val registration = firestore.collection("users").document(uid).addSnapshotListener { snapshot, _ ->
            trySend(statsFrom(snapshot?.get("stats") as? Map<*, *>))
        }
        awaitClose { registration.remove() }
    }

    private fun newProfile(user: Session.SignedIn): Map<String, Any?> = mapOf(
        "displayName" to user.displayName,
        "photoUrl" to user.photoUrl,
        "email" to user.email,
        "lang" to Locale.getDefault().language,
        "timezone" to TimeZone.getDefault().id,
        "deviceModel" to Build.MODEL,
        "usageAccess" to false,
        "createdAt" to FieldValue.serverTimestamp(),
    )
}

internal fun statsFrom(map: Map<*, *>?): TesterStats {
    fun n(key: String) = ((map?.get(key) as? Number)?.toInt() ?: 0).coerceAtLeast(0)
    return TesterStats(
        testsStarted = n("testsStarted"),
        testsCompleted = n("testsCompleted"),
        testsFailed = n("testsFailed"),
        testsLeft = n("testsLeft"),
        daysPassed = n("daysPassed"),
    )
}
