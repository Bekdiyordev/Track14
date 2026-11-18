package uz.beko404.track14.data.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import java.security.SecureRandom
import java.util.Base64

/**
 * Shows the Google account picker (Credential Manager) and returns a Google ID token.
 * Short-lived: create it with an Activity context, call [getIdToken] and drop it.
 */
class GoogleIdTokenClient(
    private val context: Context,
    private val webClientId: String,
) {
    suspend fun getIdToken(): DomainResult<String> {
        if (webClientId.isBlank() || webClientId.startsWith(PLACEHOLDER_PREFIX)) {
            return DomainResult.Failure(DomainError.Unknown("Google web client ID is not configured"))
        }
        val option = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setNonce(newNonce())
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        return try {
            val credential = CredentialManager.create(context).getCredential(context, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                DomainResult.Success(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                DomainResult.Failure(DomainError.Unknown())
            }
        } catch (e: GetCredentialCancellationException) {
            DomainResult.Failure(DomainError.Cancelled)
        } catch (e: NoCredentialException) {
            DomainResult.Failure(DomainError.NoGoogleAccount)
        } catch (e: GetCredentialException) {
            DomainResult.Failure(DomainError.Unknown())
        } catch (e: GoogleIdTokenParsingException) {
            DomainResult.Failure(DomainError.Unknown())
        }
    }

    private fun newNonce(): String {
        val bytes = ByteArray(NONCE_BYTES).also { SecureRandom().nextBytes(it) }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private companion object {
        const val PLACEHOLDER_PREFIX = "REPLACE"
        const val NONCE_BYTES = 32
    }
}
