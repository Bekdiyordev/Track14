package uz.beko404.track14

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import uz.beko404.track14.core.billing.PurchaseVerifier
import uz.beko404.track14.core.notifications.Channels
import uz.beko404.track14.core.notifications.PushRegistrar
import uz.beko404.track14.core.security.AppCheckInstaller
import uz.beko404.track14.core.work.DailyCheckWorker
import javax.inject.Inject

@HiltAndroidApp
class Track14Application : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var purchaseVerifier: PurchaseVerifier

    @Inject lateinit var pushRegistrar: PushRegistrar

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        // Firebase is already initialised by its content provider at this point.
        AppCheckInstaller.install(this)
        DailyCheckWorker.schedule(this)
        purchaseVerifier.start()
        Channels.create(this)
        pushRegistrar.start()
    }
}
