package uz.beko404.track14.core.device

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import uz.beko404.track14.domain.model.PlayLinks

/** Every outgoing link passes the PlayLinks checks first; nothing else is opened. */
object ExternalLinks {

    /** Opens the tested app. Launching does not need package visibility, only querying does. */
    fun openApp(context: Context, packageName: String): Boolean {
        if (!PlayLinks.isValidPackageName(packageName)) return false
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, intent)
    }

    fun openStorePage(context: Context, packageName: String): Boolean =
        PlayLinks.isValidPackageName(packageName) && openUrl(context, PlayLinks.storeUrl(packageName))

    fun openOptIn(context: Context, url: String): Boolean = PlayLinks.isValidOptInUrl(url) && openUrl(context, url)

    /** Only for our own constant links (for example the testers' Google Group). */
    fun openTrustedUrl(context: Context, url: String): Boolean = url.startsWith("https://") && openUrl(context, url)

    /** [email] must already be validated (SupportConfig.safeEmail). */
    fun openEmail(context: Context, email: String): Boolean =
        start(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    fun openUsageAccessSettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun openUrl(context: Context, url: String): Boolean =
        start(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}
