package uz.beko404.track14.domain.repository

/** Failure kinds the UI can react to without knowing Firebase. */
sealed interface DomainError {
    data object Network : DomainError

    /** The service is in maintenance mode (config/flags.maintenance). */
    data object Maintenance : DomainError
    data object Unauthenticated : DomainError
    data object PermissionDenied : DomainError
    data object NotFound : DomainError
    data object Blocked : DomainError

    /** User closed the Google account picker. Not an error to show. */
    data object Cancelled : DomainError
    data object NoGoogleAccount : DomainError
    /** Server refused the action; [reason] is a stable code such as `max_active_tests`. */
    data class Rejected(val reason: String) : DomainError
    data class Unknown(val message: String? = null) : DomainError
}

sealed interface DomainResult<out T> {
    data class Success<T>(val value: T) : DomainResult<T>
    data class Failure(val error: DomainError) : DomainResult<Nothing>
}

inline fun <T, R> DomainResult<T>.map(transform: (T) -> R): DomainResult<R> = when (this) {
    is DomainResult.Success -> DomainResult.Success(transform(value))
    is DomainResult.Failure -> this
}
