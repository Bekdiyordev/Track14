package uz.beko404.track14.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import uz.beko404.track14.MainActivity
import uz.beko404.track14.R
import uz.beko404.track14.domain.repository.AuthRepository
import uz.beko404.track14.domain.repository.NotificationsRepository
import uz.beko404.track14.domain.repository.Session
import javax.inject.Inject
import javax.inject.Singleton

/** Channel ids must match channelFor() in notify.ts. */
object Channels {
    const val REMINDERS = "reminders"
    const val UPDATES = "updates"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT),
                NotificationChannel(UPDATES, context.getString(R.string.channel_updates), NotificationManager.IMPORTANCE_DEFAULT),
            ),
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

/** Saves the FCM token for the signed-in user whenever the session or the token changes. */
@Singleton
class PushRegistrar @Inject constructor(
    private val auth: AuthRepository,
    private val notifications: NotificationsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start() {
        // Broadcasts from the admin app (adminSendPush, target "all") go to this topic.
        FirebaseMessaging.getInstance().subscribeToTopic(BROADCAST_TOPIC)
        scope.launch {
            auth.session.filterIsInstance<Session.SignedIn>().distinctUntilChanged().collect { s ->
                runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()?.let {
                    notifications.registerToken(s.uid, it)
                }
            }
        }
    }

    private companion object {
        /** Must match BROADCAST_TOPIC in admin.ts. */
        const val BROADCAST_TOPIC = "all"
    }

    fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        scope.launch { notifications.registerToken(uid, token) }
    }
}

@AndroidEntryPoint
class Track14MessagingService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushRegistrar

    override fun onNewToken(token: String) {
        registrar.onNewToken(token)
    }

    /** Background messages are shown by the system; this handles the app being in the foreground. */
    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification ?: return
        if (!Channels.canPost(this)) return
        val channel = n.channelId ?: Channels.UPDATES
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(this, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(n.title)
            .setContentText(n.body)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        @Suppress("MissingPermission") // checked by Channels.canPost
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
