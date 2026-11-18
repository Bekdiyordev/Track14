package uz.beko404.track14.admin.logic

/** Staff role from the `role` custom claim. Mirrors requireRole() in admin.ts. */
enum class Role(val claim: String) {
    SUPERADMIN("superadmin"),
    MODERATOR("moderator"),
    SUPPORT("support");

    val canBlockUsers: Boolean get() = this == SUPERADMIN || this == MODERATOR
    val canSuspendApps: Boolean get() = this == SUPERADMIN || this == MODERATOR
    val canAdjustCoins: Boolean get() = this == SUPERADMIN
    val canEditConfig: Boolean get() = this == SUPERADMIN
    val canSetRoles: Boolean get() = this == SUPERADMIN
    val canSendPush: Boolean get() = this == SUPERADMIN || this == SUPPORT
    val canResolveReports: Boolean get() = true

    companion object {
        fun fromClaim(value: Any?): Role? = entries.firstOrNull { it.claim == value }
    }
}

/**
 * Client-side copies of the zod schemas in admin.ts. They only give early, friendly
 * errors: the Functions validate everything again.
 */
object AdminRules {
    private val UID = Regex("^[A-Za-z0-9]{1,128}$")
    private val EMAIL = Regex("^[A-Za-z0-9._%+-]{1,64}@[A-Za-z0-9.-]{1,100}\\.[A-Za-z]{2,}$")
    const val MAX_ADJUST = 100_000
    const val MAX_PUSH_USERS = 500
    const val MAX_SCHEDULE_DAYS = 30L

    fun isUid(value: String): Boolean = UID.matches(value)

    /** Parsed amount, or null when it is not an allowed correction. */
    fun adjustAmount(text: String): Int? =
        text.trim().replace(" ", "").toIntOrNull()?.takeIf { it != 0 && it in -MAX_ADJUST..MAX_ADJUST }

    fun isReason(text: String): Boolean = text.trim().length in 3..300

    fun isPushTitle(text: String): Boolean = text.trim().length in 1..65
    fun isPushBody(text: String): Boolean = text.trim().length in 1..240

    /** "uid1, uid2 uid3" -> list; null if any entry is not a uid or the list is empty / too long. */
    fun parseUids(text: String): List<String>? {
        val ids = text.split(',', ' ', '\n', ';').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        return ids.takeIf { it.isNotEmpty() && it.size <= MAX_PUSH_USERS && it.all(::isUid) }
    }

    fun isValidSendAt(sendAtMillis: Long?, nowMillis: Long): Boolean {
        if (sendAtMillis == null) return true
        return sendAtMillis >= nowMillis - 60_000 && sendAtMillis <= nowMillis + MAX_SCHEDULE_DAYS * 86_400_000
    }

    fun isValidVersion(minBuild: Long?, latestBuild: Long?): Boolean =
        minBuild != null && latestBuild != null && minBuild >= 1 && latestBuild >= minBuild

    fun isTelegram(url: String): Boolean = url.isEmpty() || (url.startsWith("https://t.me/") && url.length <= 200)
    fun isGroupUrl(url: String): Boolean = url.isEmpty() || (url.startsWith("https://groups.google.com/") && url.length <= 200)
    fun isHttps(url: String): Boolean = url.isEmpty() || (url.startsWith("https://") && url.length <= 300)
    fun isEmail(value: String): Boolean = value.isEmpty() || EMAIL.matches(value)

    /** Economy keys accepted by adminSetConfig("economy"). */
    val ECONOMY_KEYS = listOf(
        "standardFee", "proFee", "standardRewardPerDay", "proRewardPerDay", "completionBonus",
        "feedbackReward", "failedPenalty", "leftPenalty", "welcomeBonus", "maxActiveTests",
        "testersNeeded", "feature1Price", "feature3Price", "feature7Price",
    )

    /** Non-negative integers only; maxActiveTests and testersNeeded are 1..100. Returns null if any value is bad. */
    fun parseEconomy(values: Map<String, String>): Map<String, Int>? {
        val out = mutableMapOf<String, Int>()
        for ((key, raw) in values) {
            if (key !in ECONOMY_KEYS || raw.isBlank()) continue
            val n = raw.trim().toIntOrNull() ?: return null
            val ok = if (key == "maxActiveTests" || key == "testersNeeded") n in 1..100 else n >= 0
            if (!ok) return null
            out[key] = n
        }
        return out
    }
}
