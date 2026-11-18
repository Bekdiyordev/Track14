package uz.beko404.track14.core.device

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import uz.beko404.track14.domain.rules.DailyCheckRules
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mode B (no usage permission): remembers when the tester opened an app from Track14,
 * so the time until they come back can be measured. Stored locally, excluded from backup.
 */
@Singleton
class LaunchLog @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("launch_log", Context.MODE_PRIVATE)

    fun recordLaunch(testId: String, atMillis: Long) {
        prefs.edit().putLong(testId, atMillis).apply()
    }

    /** Seconds since the last launch, or null when there is none or it is not believable. */
    fun elapsedSeconds(testId: String, nowMillis: Long): Int? {
        val at = prefs.getLong(testId, -1L).takeIf { it > 0 } ?: return null
        val seconds = ((nowMillis - at) / 1000).toInt()
        return seconds.takeIf { it in 0..DailyCheckRules.MAX_FALLBACK_SECONDS }
    }

    fun clear(testId: String) {
        prefs.edit().remove(testId).apply()
    }
}
