package uz.beko404.track14.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import uz.beko404.track14.data.check.CheckOutcome
import uz.beko404.track14.data.check.DailyCheckRunner
import uz.beko404.track14.domain.model.TestStatus
import uz.beko404.track14.domain.repository.DomainError
import uz.beko404.track14.domain.repository.DomainResult
import uz.beko404.track14.domain.repository.TestsRepository
import java.util.concurrent.TimeUnit

/**
 * Background check (mode A only): a few times a day, report today's usage for every
 * active test, so a tester who simply uses the app gets the day without opening Track14.
 */
@HiltWorker
class DailyCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val auth: FirebaseAuth,
    private val tests: TestsRepository,
    private val runner: DailyCheckRunner,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val uid = auth.currentUser?.uid ?: return Result.success()
        val active = when (val result = tests.observeMyTests(uid).first()) {
            is DomainResult.Success -> result.value.filter { it.status == TestStatus.ACTIVE }
            is DomainResult.Failure -> return if (result.error == DomainError.Network) Result.retry() else Result.success()
        }
        var retry = false
        for (test in active) {
            val outcome = runner.check(test, allowFallback = false)
            if (outcome is CheckOutcome.Failed && outcome.error == DomainError.Network) retry = true
        }
        return if (retry) Result.retry() else Result.success()
    }

    companion object {
        private const val NAME = "daily-check"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyCheckWorker>(3, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
