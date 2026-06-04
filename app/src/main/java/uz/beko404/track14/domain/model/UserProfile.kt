package uz.beko404.track14.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val displayName: String,
    val ownerScore: Int,
    val freeAppLimit: Int,
    val themeMode: ThemeMode,
    val timezoneId: String,
)

enum class ThemeMode {
    System,
    Light,
    Dark,
}
