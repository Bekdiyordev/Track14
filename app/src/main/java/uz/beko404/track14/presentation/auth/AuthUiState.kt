package uz.beko404.track14.presentation.auth

import uz.beko404.track14.domain.model.AuthSession

data class AuthUiState(
    val session: AuthSession? = null,
) {
    val isSignedIn: Boolean = session != null
    val email: String? = session?.email
}
