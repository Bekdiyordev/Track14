package uz.beko404.track14.domain.model

/**
 * Links and package names come from app owners, so they are untrusted. The app only
 * opens Play testing links and Play Store pages, never an arbitrary URL.
 */
object PlayLinks {
    private val PACKAGE = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private const val OPT_IN_PREFIX = "https://play.google.com/apps/testing/"

    fun isValidPackageName(name: String?): Boolean =
        name != null && name.length <= 150 && PACKAGE.matches(name)

    /** Closed-test opt-in link: https://play.google.com/apps/testing/<package>. */
    fun isValidOptInUrl(url: String?): Boolean =
        url != null && url.startsWith(OPT_IN_PREFIX) &&
            isValidPackageName(url.removePrefix(OPT_IN_PREFIX).substringBefore('?').trimEnd('/'))

    fun storeUrl(packageName: String): String {
        require(isValidPackageName(packageName)) { "invalid package name" }
        return "https://play.google.com/store/apps/details?id=$packageName"
    }
}
