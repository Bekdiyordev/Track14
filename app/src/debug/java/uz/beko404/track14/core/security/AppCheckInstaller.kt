package uz.beko404.track14.core.security

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

/** Debug builds use the App Check debug provider (token is registered in the Firebase console). */
object AppCheckInstaller {
    fun install(@Suppress("UNUSED_PARAMETER") app: Application) {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
