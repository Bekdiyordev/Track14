package uz.beko404.track14.domain.model

data class AddOwnerAppRequest(
    val name: String,
    val packageName: String,
    val googleGroupUrl: String,
    val playOptInUrl: String,
    val playInstallUrl: String,
    val iconUrl: String? = null,
)

sealed interface AddOwnerAppResult {
    data class Success(val app: TrackApp) : AddOwnerAppResult
    data class Failure(val message: String) : AddOwnerAppResult
}
