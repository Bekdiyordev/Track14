package uz.beko404.track14.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.ProfileRepository
import uz.beko404.track14.domain.repository.Session
import javax.inject.Inject

/** [session] is null until Firebase reports the first auth state. */
data class AuthUiState(
    val session: Session? = null,
    val isSigningIn: Boolean = false,
    val error: DomainError? = null,
)

/**
 * Optional login: browsing needs no account. Screens that need one (test, add app,
 * wallet) read this state and call [signIn] from their own "sign in" prompt.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    private val signingIn = MutableStateFlow(false)
    private val error = MutableStateFlow<DomainError?>(null)

    val state: StateFlow<AuthUiState> =
        combine(auth.session, signingIn, error) { session, busy, err ->
            AuthUiState(session = session, isSigningIn = busy, error = err)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthUiState())

    /**
     * [requestToken] shows the Google account picker. It is passed in by the screen
     * because it needs an Activity context, which the ViewModel must not hold.
     */
    fun signIn(requestToken: suspend () -> DomainResult<String>) {
        if (signingIn.value) return
        viewModelScope.launch {
            signingIn.value = true
            error.value = null
            when (val token = requestToken()) {
                is DomainResult.Failure -> reportError(token.error)
                is DomainResult.Success -> when (val result = auth.signInWithGoogle(token.value)) {
                    is DomainResult.Failure -> reportError(result.error)
                    // A failed profile write is retried on the next sign-in; it must not block login.
                    is DomainResult.Success -> profiles.ensureProfile(result.value)
                }
            }
            signingIn.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch { auth.signOut() }
    }

    fun dismissError() {
        error.value = null
    }

    private fun reportError(e: DomainError) {
        // Closing the account picker is a choice, not a failure.
        if (e != DomainError.Cancelled) error.value = e
    }
}
