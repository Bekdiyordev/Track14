package uz.beko404.track14.domain.model

data class TrackApp(
    val id: String,
    val ownerId: String,
    val name: String,
    val packageName: String,
    val googleGroupUrl: String,
    val playOptInUrl: String,
    val playInstallUrl: String,
    val status: TrackAppStatus,
    val testerCount: Int,
    val requiredTesterCount: Int,
    val rankingScore: Int,
    val ownerScore: Int,
)

enum class TrackAppStatus {
    Active,
    Paused,
    Completed,
}
