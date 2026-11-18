package uz.beko404.track14.feature

import androidx.annotation.StringRes
import uz.beko404.track14.R
import uz.beko404.track14.domain.repository.DomainError

/** One place that turns domain errors into user text. Raw server messages are never shown. */
@StringRes
fun DomainError.messageRes(): Int = when (this) {
    DomainError.Maintenance -> R.string.error_maintenance
    DomainError.Network -> R.string.error_network
    DomainError.NoGoogleAccount -> R.string.error_no_google_account
    DomainError.Blocked -> R.string.error_blocked
    DomainError.Unauthenticated -> R.string.error_sign_in_again
    is DomainError.Rejected -> when (reason) {
        "own_app" -> R.string.error_own_app
        "max_active_tests" -> R.string.error_max_active_tests
        "campaign_not_active" -> R.string.error_campaign_not_active
        "outside_test_window" -> R.string.error_outside_window
        "insufficient_balance" -> R.string.error_insufficient_balance
        "package_in_use" -> R.string.error_package_in_use
        "app_not_draft" -> R.string.error_app_not_draft
        "invalid_transition" -> R.string.error_invalid_transition
        "suspended" -> R.string.error_suspended
        "new_apps_disabled" -> R.string.error_new_apps_disabled
        else -> R.string.error_generic
    }
    else -> R.string.error_generic
}
