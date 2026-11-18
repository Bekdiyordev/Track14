package uz.beko404.track14.admin.core

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Base64

/** Google account picker -> Firebase session. Returns null when cancelled or not configured. */
suspend fun signInWithGoogle(context: Context, webClientId: String, auth: FirebaseAuth): Boolean {
    if (webClientId.isBlank() || webClientId.startsWith("REPLACE")) return false
    val nonce = ByteArray(32).also { SecureRandom().nextBytes(it) }
    val option = GetGoogleIdOption.Builder()
        .setServerClientId(webClientId)
        .setFilterByAuthorizedAccounts(false)
        .setNonce(Base64.getUrlEncoder().withoutPadding().encodeToString(nonce))
        .build()
    return runCatching {
        val credential = CredentialManager.create(context)
            .getCredential(context, GetCredentialRequest.Builder().addCredentialOption(option).build()).credential
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) return false
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await().user != null
    }.getOrDefault(false)
}
