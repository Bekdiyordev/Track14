package uz.beko404.track14.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import uz.beko404.track14.data.callFunction
import uz.beko404.track14.data.toDomainError
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.Session
import javax.inject.Inject

class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
) : AuthRepository {

    override val session: Flow<Session> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser.toSession()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogle(idToken: String): DomainResult<Session.SignedIn> =
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val user = withTimeout(30_000) { auth.signInWithCredential(credential).await().user }
            val session = user.toSession()
            if (session is Session.SignedIn) {
                DomainResult.Success(session)
            } else {
                DomainResult.Failure(DomainError.Unauthenticated)
            }
        } catch (_: TimeoutCancellationException) {
            DomainResult.Failure(DomainError.Network)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DomainResult.Failure(e.toDomainError())
        }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun deleteAccount(): DomainResult<Unit> {
        val result = functions.callFunction("deleteAccount", mapOf("confirm" to "DELETE")) { Unit }
        // The server has deleted the Auth user; drop the local session too.
        if (result is DomainResult.Success) auth.signOut()
        return result
    }

    private fun FirebaseUser?.toSession(): Session =
        if (this == null) {
            Session.SignedOut
        } else {
            Session.SignedIn(
                uid = uid,
                displayName = displayName,
                email = email,
                photoUrl = photoUrl?.toString(),
            )
        }
}
