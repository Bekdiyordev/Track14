package uz.beko404.track14.admin.core

import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

object AppCheckInstaller {
    fun install() {
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(DebugAppCheckProviderFactory.getInstance())
    }
}
