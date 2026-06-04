package uz.beko404.track14.domain.model

data class AuthSession(
    val userId: String,
    val email: String,
    val displayName: String,
)
