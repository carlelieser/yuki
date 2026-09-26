package app.yuki.feature.updates

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val PERIODIC_UPDATE_CHECK = "update_check"
internal const val IMMEDIATE_UPDATE_CHECK = "update_check_now"
internal const val UPDATE_CHECK_INTERVAL_HOURS = 12L

internal interface UpdateCheckWork {
    suspend fun schedule()

    suspend fun runOnce()

    suspend fun cancel()
}

internal class WorkManagerUpdateCheckWork @Inject constructor(
    private val workManager: WorkManager,
) : UpdateCheckWork {
    override suspend fun schedule() {
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            UPDATE_CHECK_INTERVAL_HOURS,
            TimeUnit.HOURS,
        )
            .setConstraints(updateCheckConstraints())
            .build()

        workManager
            .enqueueUniquePeriodicWork(PERIODIC_UPDATE_CHECK, ExistingPeriodicWorkPolicy.KEEP, request)
            .await()
    }

    override suspend fun runOnce() {
        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>()
            .setConstraints(updateCheckConstraints())
            .build()

        workManager
            .enqueueUniqueWork(IMMEDIATE_UPDATE_CHECK, ExistingWorkPolicy.REPLACE, request)
            .await()
    }

    override suspend fun cancel() {
        workManager.cancelUniqueWork(PERIODIC_UPDATE_CHECK).await()
        workManager.cancelUniqueWork(IMMEDIATE_UPDATE_CHECK).await()
    }
}

private fun updateCheckConstraints(): Constraints = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .setRequiresBatteryNotLow(true)
    .build()

@Module
@InstallIn(SingletonComponent::class)
internal abstract class UpdateCheckWorkModule {
    @Binds
    abstract fun bindWork(work: WorkManagerUpdateCheckWork): UpdateCheckWork
}
