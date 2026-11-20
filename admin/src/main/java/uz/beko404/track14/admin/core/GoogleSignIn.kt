package uz.beko404.track14.admin.core

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.firebase.FirebaseNetworkException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import java.security.SecureRandom
import java.util.Base64

sealed interface GoogleSignInResult {
    data object SignedIn : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failed(val message: String) : GoogleSignInResult
}

/** Google account picker -> Firebase session, with a retryable error for the login screen. */
suspend fun signInWithGoogle(context: Context, webClientId: String, auth: FirebaseAuth): GoogleSignInResult {
    if (webClientId.isBlank() || webClientId.startsWith("REPLACE")) {
        return GoogleSignInResult.Failed("Google kirish sozlamasi to'ldirilmagan.")
    }
    val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(false)
        .setNonce(Base64.getUrlEncoder().withoutPadding().encodeToString(nonce))
        .build()
    return try {
        val credential = CredentialManager.create(context)
            .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            return GoogleSignInResult.Failed("Google akkaunt ma'lumoti olinmadi. Qayta urinib ko'ring.")
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val user = withTimeout(60_000) {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user
        }
        if (user != null) GoogleSignInResult.SignedIn
        else GoogleSignInResult.Failed("Kirish yakunlanmadi. Qayta urinib ko'ring.")
    } catch (_: GetCredentialCancellationException) {
        GoogleSignInResult.Cancelled
    } catch (_: NoCredentialException) {
        GoogleSignInResult.Failed("Telefonga Google akkaunt qo'shing.")
    } catch (_: TimeoutCancellationException) {
        GoogleSignInResult.Failed("Kirish vaqti tugadi. Internetni tekshirib, qayta urinib ko'ring.")
    } catch (e: CancellationException) {
        throw e
    } catch (_: FirebaseNetworkException) {
        GoogleSignInResult.Failed("Internetga ulanib bo'lmadi. Qayta urinib ko'ring.")
    } catch (e: FirebaseAuthException) {
        GoogleSignInResult.Failed("Firebase kirish xatosi: ${e.errorCode}")
    } catch (_: Exception) {
        GoogleSignInResult.Failed("Google orqali kirib bo'lmadi. Qayta urinib ko'ring.")
    }
}
