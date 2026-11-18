package uz.beko404.track14.data.wallet

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import uz.beko404.track14.data.apps.toMillis
import uz.beko404.track14.data.callFunction
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.model.Coins
import uz.beko404.track14.domain.model.TxType
import uz.beko404.track14.domain.model.WalletTransaction
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.PurchaseResult
import uz.beko404.track14.domain.repository.WalletRepository
import javax.inject.Inject

/** Read-only view of `wallets/{uid}` and the ledger. The security rules forbid client writes. */
class FirestoreWalletRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
) : WalletRepository {

    override fun observeBalance(uid: String): Flow<DomainResult<Coins>> = callbackFlow {
        val registration = firestore.collection("wallets").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(walletBalanceFrom(snapshot?.data)))
                }
            }
        awaitClose { registration.remove() }
    }

    override fun observeTransactions(uid: String): Flow<DomainResult<List<WalletTransaction>>> = callbackFlow {
        val registration = firestore.collection("transactions")
            .whereEqualTo("uid", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(HISTORY_SIZE)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(DomainResult.Failure(error.toDomainError()))
                } else {
                    trySend(DomainResult.Success(snapshot?.documents.orEmpty().mapNotNull { d -> d.data?.let { transactionFrom(d.id, it) } }))
                }
            }
        awaitClose { registration.remove() }
    }

    override suspend fun verifyPurchase(productId: String, purchaseToken: String): DomainResult<PurchaseResult> =
        functions.callFunction("verifyPurchase", mapOf("productId" to productId, "purchaseToken" to purchaseToken)) { data ->
            when (data["status"]) {
                "credited" -> PurchaseResult.CREDITED
                "pending" -> PurchaseResult.PENDING
                else -> null
            }
        }

    private companion object {
        const val HISTORY_SIZE = 50L
    }
}

/** Missing wallet = zero balance (the wallet document is created on first login by a Function). */
internal fun walletBalanceFrom(data: Map<String, Any?>?): Coins =
    Coins((data?.get("balance") as? Number)?.toLong() ?: 0L)

internal fun transactionFrom(id: String, data: Map<String, Any?>): WalletTransaction? {
    val type = TxType.fromWire(data["type"] as? String) ?: return null
    val amount = (data["amount"] as? Number)?.toLong() ?: return null
    return WalletTransaction(
        id = id,
        type = type,
        amount = Coins(amount),
        refId = data["refId"] as? String ?: "",
        createdAtMillis = data["createdAt"].toMillis(),
    )
}
