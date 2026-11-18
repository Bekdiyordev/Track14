package uz.beko404.track14.domain.model

/**
 * App ids arrive from navigation and deep links, so they are untrusted input.
 * A Firestore id with "/" would address a different path; allow only plain ids.
 */
object AppIds {
    private val VALID = Regex("^[A-Za-z0-9_-]{1,64}$")

    fun isValid(id: String?): Boolean = id != null && VALID.matches(id)
}
