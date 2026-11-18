package uz.beko404.track14.core.security

import android.app.Application
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

/** Release builds prove they are the genuine app on a genuine device via Play Integrity. */
object AppCheckInstaller {
    fun install(@Suppress("UNUSED_PARAMETER") app: Application) {
        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(PlayIntegrityAppCheckProviderFactory.getInstance())
    }
}
