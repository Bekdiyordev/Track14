package uz.beko404.track14.data.economy

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.EconomyConfig
import uz.beko404.track14.domain.repository.EconomyRepository
import javax.inject.Inject

/** `config/economy` is public; the server reads the same document, so prices shown = prices charged. */
class FirestoreEconomyRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : EconomyRepository {
    override fun observe(): Flow<EconomyConfig> = callbackFlow {
        val registration = firestore.document("config/economy").addSnapshotListener { snapshot, _ ->
            trySend(economyFrom(snapshot?.data))
        }
        awaitClose { registration.remove() }
    }
}

/** Mirrors mergeEconomy in economy.ts: only non-negative integers override the defaults. */
internal fun economyFrom(data: Map<String, Any?>?): EconomyConfig {
    val d = EconomyConfig()
    fun pick(key: String, fallback: Coins): Coins {
        val v = data?.get(key) as? Number ?: return fallback
        val long = v.toLong()
        return if (long >= 0 && long.toDouble() == v.toDouble()) Coins(long) else fallback
    }
    return EconomyConfig(
        standardFee = pick("standardFee", d.standardFee),
        proFee = pick("proFee", d.proFee),
        standardRewardPerDay = pick("standardRewardPerDay", d.standardRewardPerDay),
        proRewardPerDay = pick("proRewardPerDay", d.proRewardPerDay),
        completionBonus = pick("completionBonus", d.completionBonus),
        feedbackReward = pick("feedbackReward", d.feedbackReward),
        failedPenalty = pick("failedPenalty", d.failedPenalty),
        leftPenalty = pick("leftPenalty", d.leftPenalty),
        feature1Price = pick("feature1Price", d.feature1Price),
        feature3Price = pick("feature3Price", d.feature3Price),
        feature7Price = pick("feature7Price", d.feature7Price),
    )
}
