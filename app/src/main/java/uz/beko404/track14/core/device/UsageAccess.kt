package uz.beko404.track14.core.device

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads how long an app was in the foreground today (mode A, plan section 4).
 * Needs the special "Usage access" permission that the user grants in system settings.
 */
@Singleton
class UsageAccess @Inject constructor(@ApplicationContext private val context: Context) {

    fun hasAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground seconds of [packageName] since local midnight in [zone]; 0 without permission. */
    fun foregroundSecondsToday(packageName: String, zone: ZoneId, nowMillis: Long = System.currentTimeMillis()): Int {
        if (!hasAccess()) return 0
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return 0
        val midnight = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        val events = manager.queryEvents(midnight, nowMillis) ?: return 0
        val event = UsageEvents.Event()
        var resumedAt: Long? = null
        var totalMillis = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName != packageName) continue
            when (event.eventType) {
                RESUMED -> if (resumedAt == null) resumedAt = event.timeStamp
                PAUSED -> resumedAt?.let { totalMillis += event.timeStamp - it; resumedAt = null }
            }
        }
        resumedAt?.let { totalMillis += nowMillis - it }
        return (totalMillis / 1000).coerceIn(0, Int.MAX_VALUE.toLong()).toInt()
    }

    private companion object {
        // ACTIVITY_RESUMED / ACTIVITY_PAUSED (API 29) share values with the older MOVE_TO_* constants.
        const val RESUMED = 1
        const val PAUSED = 2
    }
}
