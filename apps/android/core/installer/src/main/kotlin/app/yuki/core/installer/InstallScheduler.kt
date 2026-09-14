package app.yuki.core.installer

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class InstallScheduler @Inject internal constructor(
    private val workManager: WorkManager,
    private val progress: InstallProgressStore,
) {
    fun observe(githubRepoId: Long): Flow<InstallProgress?> = progress.observe(githubRepoId)

    fun observeActive(): Flow<List<InstallProgress>> = progress.observeActive()

    fun start(request: InstallRequest) {
        workManager.enqueueUniqueWork(
            installWorkName(request.target.githubRepoId),
            ExistingWorkPolicy.REPLACE,
            request.toWorkRequest(),
        )
    }

    suspend fun cancel(githubRepoId: Long) {
        workManager.cancelUniqueWork(installWorkName(githubRepoId))
        progress.clear(githubRepoId)
    }

    suspend fun forget(githubRepoId: Long) = progress.clear(githubRepoId)
}

private fun InstallRequest.toWorkRequest() = OneTimeWorkRequestBuilder<InstallWorker>()
    .setInputData(toWorkData())
    .setConstraints(INSTALL_CONSTRAINTS)
    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, INSTALL_BACKOFF)
    .addTag(installWorkName(target.githubRepoId))
    .build()

private val INSTALL_CONSTRAINTS = Constraints.Builder()
    .setRequiredNetworkType(NetworkType.CONNECTED)
    .build()

private val INSTALL_BACKOFF: Duration = Duration.ofSeconds(30)
