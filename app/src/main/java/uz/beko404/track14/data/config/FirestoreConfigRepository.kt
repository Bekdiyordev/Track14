package uz.beko404.track14.data.config

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import uz.beko404.track14.domain.model.FeatureFlags
import uz.beko404.track14.domain.model.SupportConfig
import uz.beko404.track14.domain.model.VersionConfig
import uz.beko404.track14.domain.repository.ConfigRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreConfigRepository @Inject constructor(private val firestore: FirebaseFirestore) : ConfigRepository {

    private fun <T> doc(path: String, map: (Map<String, Any?>?) -> T): Flow<T> = callbackFlow {
        val registration = firestore.document(path).addSnapshotListener { snapshot, _ -> trySend(map(snapshot?.data)) }
        awaitClose { registration.remove() }
    }.distinctUntilChanged()

    override fun version(): Flow<VersionConfig> = doc("config/app_version", ::versionFrom)
    override fun support(): Flow<SupportConfig> = doc("config/support", ::supportFrom)
    override fun flags(): Flow<FeatureFlags> = doc("config/flags", ::flagsFrom)

    override fun blockedReason(uid: String): Flow<String?> = doc("users/$uid") { data ->
        blockedReasonFrom(data, System.currentTimeMillis())
    }
}

/** Reason while a block is in force; a temporary block past `blockedUntil` no longer counts. */
internal fun blockedReasonFrom(data: Map<String, Any?>?, nowMillis: Long): String? {
    if (data?.get("blocked") != true) return null
    val until = (data["blockedUntil"] as? com.google.firebase.Timestamp)?.toDate()?.time
    if (until != null && until <= nowMillis) return null
    return (data["blockedReason"] as? String).orEmpty()
}

private fun Map<String, Any?>.long(key: String, fallback: Long) = (this[key] as? Number)?.toLong()?.takeIf { it > 0 } ?: fallback
private fun Map<String, Any?>.text(key: String, max: Int) = (this[key] as? String)?.take(max).orEmpty()

internal fun versionFrom(data: Map<String, Any?>?): VersionConfig {
    if (data == null) return VersionConfig()
    val min = data.long("minBuild", 1)
    return VersionConfig(minBuild = min, latestBuild = maxOf(min, data.long("latestBuild", min)), message = data.text("message", 300))
}

internal fun supportFrom(data: Map<String, Any?>?): SupportConfig {
    if (data == null) return SupportConfig()
    return SupportConfig(
        telegram = data.text("telegram", 200),
        email = data.text("email", 120),
        groupUrl = data.text("groupUrl", 200),
        faqUrl = data.text("faqUrl", 300),
    )
}

internal fun flagsFrom(data: Map<String, Any?>?): FeatureFlags {
    if (data == null) return FeatureFlags()
    return FeatureFlags(
        maintenance = data["maintenance"] == true,
        maintenanceMessage = data.text("maintenanceMessage", 300),
        newApps = data["newApps"] != false,
        purchases = data["purchases"] != false,
    )
}
