package uz.beko404.track14.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.functions.FirebaseFunctionsException
import uz.beko404.track14.domain.repository.DomainError

/** Translates Firebase exceptions to domain errors. Raw messages are never shown to users. */
internal fun Throwable.toDomainError(): DomainError = when (this) {
    is FirebaseNetworkException -> DomainError.Network
    is FirebaseAuthInvalidUserException -> DomainError.Blocked
    is FirebaseFirestoreException -> when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED -> DomainError.PermissionDenied
        FirebaseFirestoreException.Code.UNAUTHENTICATED -> DomainError.Unauthenticated
        FirebaseFirestoreException.Code.NOT_FOUND -> DomainError.NotFound
        FirebaseFirestoreException.Code.UNAVAILABLE -> DomainError.Network
        else -> DomainError.Unknown()
    }
    is FirebaseFunctionsException -> when (code) {
        FirebaseFunctionsException.Code.UNAUTHENTICATED -> DomainError.Unauthenticated
        FirebaseFunctionsException.Code.UNAVAILABLE ->
            when (message) {
                "maintenance" -> DomainError.Maintenance
                "new_apps_disabled" -> DomainError.Rejected("new_apps_disabled")
                else -> DomainError.Network
            }
        FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> DomainError.Network
        FirebaseFunctionsException.Code.NOT_FOUND -> DomainError.NotFound
        FirebaseFunctionsException.Code.PERMISSION_DENIED ->
            if (message == "blocked") DomainError.Blocked else DomainError.PermissionDenied
        // Our functions send stable reason codes (e.g. "max_active_tests") as the message.
        else -> DomainError.Rejected(message.orEmpty().take(64))
    }
    else -> DomainError.Unknown()
}
