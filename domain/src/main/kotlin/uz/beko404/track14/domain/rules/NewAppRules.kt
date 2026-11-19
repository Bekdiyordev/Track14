package uz.beko404.track14.domain.rules

import uz.beko404.track14.domain.model.AppCategories
import uz.beko404.track14.domain.model.PlayLinks

data class NewApp(
    val name: String,
    val packageName: String,
    val category: String,
    val shortDesc: String,
    val optInUrl: String,
    /** Filled when the form was prefilled from an APK. */
    val versionName: String = "",
    val fromApk: Boolean = false,
)

enum class NewAppField { NAME, PACKAGE, CATEGORY, DESCRIPTION, OPT_IN }

/** Same limits as firestore.rules, so the form fails before the server does. */
object NewAppRules {
    const val MAX_NAME = 60
    const val MAX_DESC = 300

    fun normalize(app: NewApp) = app.copy(
        name = app.name.trim(),
        packageName = app.packageName.trim(),
        shortDesc = app.shortDesc.trim(),
        optInUrl = app.optInUrl.trim(),
    )

    fun errors(input: NewApp): Set<NewAppField> {
        val app = normalize(input)
        return buildSet {
            if (app.name.isEmpty() || app.name.length > MAX_NAME) add(NewAppField.NAME)
            if (!PlayLinks.isValidPackageName(app.packageName)) add(NewAppField.PACKAGE)
            if (app.category !in AppCategories.all) add(NewAppField.CATEGORY)
            if (app.shortDesc.length > MAX_DESC) add(NewAppField.DESCRIPTION)
            if (!PlayLinks.isValidOptInUrl(app.optInUrl)) add(NewAppField.OPT_IN)
        }
    }

    /** The closed-test opt-in link Play generates for a package. */
    fun defaultOptInUrl(packageName: String): String = "https://play.google.com/apps/testing/${packageName.trim()}"
}
