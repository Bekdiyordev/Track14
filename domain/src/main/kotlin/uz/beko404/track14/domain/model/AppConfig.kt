package uz.beko404.track14.domain.model

/** `config/app_version`, written by adminSetConfig. Builds are versionCode values. */
data class VersionConfig(
    val minBuild: Long = 1,
    val latestBuild: Long = 1,
    val message: String = "",
)

/** `config/support`. Every link is re-checked before it is opened. */
data class SupportConfig(
    val telegram: String = "",
    val email: String = "",
    val groupUrl: String = "",
    val faqUrl: String = "",
) {
    val safeTelegram: String? get() = telegram.takeIf { it.startsWith("https://t.me/") && it.length <= 200 }
    val safeGroupUrl: String? get() = groupUrl.takeIf { it.startsWith("https://groups.google.com/") && it.length <= 200 }
    val safeFaqUrl: String? get() = faqUrl.takeIf { it.startsWith("https://") && it.length <= 300 }
    val safeEmail: String? get() = email.takeIf { EMAIL.matches(it) }

    private companion object {
        val EMAIL = Regex("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,100}\\.[A-Za-z]{2,}$")
    }
}

/** `config/flags`. Missing document = everything on, no maintenance. */
data class FeatureFlags(
    val maintenance: Boolean = false,
    val maintenanceMessage: String = "",
    val newApps: Boolean = true,
    val purchases: Boolean = true,
)

enum class UpdateState { NONE, OPTIONAL, REQUIRED }

/** What the whole app shows before any screen. Order of checks = order of importance. */
sealed interface GateState {
    data object Loading : GateState
    data class UpdateRequired(val message: String) : GateState
    data class Maintenance(val message: String) : GateState
    data class Blocked(val reason: String) : GateState
    data class Open(val optionalUpdate: Boolean) : GateState
}

object GateRules {
    fun update(currentBuild: Long, config: VersionConfig): UpdateState = when {
        currentBuild < config.minBuild -> UpdateState.REQUIRED
        currentBuild < config.latestBuild -> UpdateState.OPTIONAL
        else -> UpdateState.NONE
    }

    /** A required update wins over maintenance, so even old clients can leave a broken state. */
    fun gate(currentBuild: Long, version: VersionConfig, flags: FeatureFlags, blockedReason: String?): GateState {
        val update = update(currentBuild, version)
        return when {
            update == UpdateState.REQUIRED -> GateState.UpdateRequired(version.message)
            flags.maintenance -> GateState.Maintenance(flags.maintenanceMessage)
            blockedReason != null -> GateState.Blocked(blockedReason)
            else -> GateState.Open(optionalUpdate = update == UpdateState.OPTIONAL)
        }
    }
}
