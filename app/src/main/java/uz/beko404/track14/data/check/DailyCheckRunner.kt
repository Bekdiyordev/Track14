package uz.beko404.track14.data.check

import uz.beko404.track14.core.device.LaunchLog
import uz.beko404.track14.core.device.UsageAccess
import uz.beko404.track14.domain.model.TesterTest
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.TestsRepository
import uz.beko404.track14.domain.rules.CheckMode
import uz.beko404.track14.domain.rules.DailyCheckRules
import uz.beko404.track14.domain.time.Clock
import java.time.ZoneId
import javax.inject.Inject

sealed interface CheckOutcome {
    data object NotNeeded : CheckOutcome
    data class NotEnough(val seconds: Int) : CheckOutcome
    data class Passed(val day: Int, val completed: Boolean) : CheckOutcome
    data class Failed(val error: DomainError) : CheckOutcome
}

/**
 * Measures today's time for one test and reports it to `verifyDay`.
 * [allowFallback] is false in the background worker: mode B needs the tester's own launch.
 */
class DailyCheckRunner @Inject constructor(
    private val usage: UsageAccess,
    private val launchLog: LaunchLog,
    private val tests: TestsRepository,
    private val clock: Clock,
) {
    suspend fun check(test: TesterTest, allowFallback: Boolean): CheckOutcome {
        val now = clock.nowMillis()
        if (!test.needsCheckToday(now) || test.packageName.isEmpty()) return CheckOutcome.NotNeeded

        val zone = runCatching { ZoneId.of(test.timezone) }.getOrDefault(ZoneId.systemDefault())
        val (seconds, mode) = when {
            usage.hasAccess() -> usage.foregroundSecondsToday(test.packageName, zone, now) to CheckMode.USAGE_ACCESS
            allowFallback -> (launchLog.elapsedSeconds(test.id, now) ?: return CheckOutcome.NotNeeded) to CheckMode.FALLBACK
            else -> return CheckOutcome.NotNeeded
        }
        if (!DailyCheckRules.isPassed(seconds)) return CheckOutcome.NotEnough(seconds)

        return when (val result = tests.verifyDay(test.id, seconds, mode)) {
            is DomainResult.Success -> {
                launchLog.clear(test.id)
                if (result.value.passed) {
                    CheckOutcome.Passed(result.value.day, result.value.completed)
                } else {
                    CheckOutcome.NotEnough(seconds)
                }
            }
            is DomainResult.Failure -> CheckOutcome.Failed(result.error)
        }
    }
}
