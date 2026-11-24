package work.ahmedhatem.mawaqit.hijri

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.util.concurrent.TimeUnit

/** Runs one [HijriSync]. A refused file is not retried (the next daily run will look again). */
class HijriSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = store(applicationContext)
        val now = System.currentTimeMillis()
        // Work started when the app was opened checks here (off the main thread) whether a sync is due at all.
        if (shouldSkip(inputData.getBoolean(ONLY_IF_STALE, false), runAttemptCount, store.state(), now)) return Result.success()
        return resultOf(HijriSync(store, HijriFetcher()).run(now), runAttemptCount)
    }

    companion object {
        private const val PERIODIC_NAME = "hijri-sync-daily"
        private const val NOW_NAME = "hijri-sync-now"
        private const val ONLY_IF_STALE = "only_if_stale"
        private const val MAX_ATTEMPTS = 5

        fun store(context: Context) = HijriStore(File(context.filesDir, "hijri"))

        /**
         * Work queued by an app start skips when the last attempt is recent. Only the first run decides: a retry of an
         * attempt that failed must go on, even though that attempt was recorded.
         */
        internal fun shouldSkip(onlyIfStale: Boolean, runAttempt: Int, state: HijriSyncState, nowMillis: Long): Boolean =
            onlyIfStale && runAttempt == 0 && !HijriSync.isStale(state, nowMillis)

        internal fun resultOf(outcome: SyncOutcome, attempt: Int): Result = when (outcome) {
            SyncOutcome.Updated, SyncOutcome.NotModified, is SyncOutcome.Rejected -> Result.success()
            is SyncOutcome.NetworkError, is SyncOutcome.StorageError ->
                if (attempt + 1 < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }

        private val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Once a day, when there is a network. Safe to call at every start: an existing schedule is kept. */
        fun scheduleDaily(context: Context) {
            val request = PeriodicWorkRequestBuilder<HijriSyncWorker>(1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** When the app is opened: queue a check; the worker syncs only if the last attempt is older than a day. */
        fun syncIfStale(context: Context) {
            val request = OneTimeWorkRequestBuilder<HijriSyncWorker>()
                .setInputData(workDataOf(ONLY_IF_STALE to true))
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(NOW_NAME, ExistingWorkPolicy.KEEP, request)
        }
    }
}
