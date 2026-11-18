package uz.beko404.track14.admin

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import uz.beko404.track14.admin.core.AppCheckInstaller

@HiltAndroidApp
class AdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCheckInstaller.install()
    }
}
