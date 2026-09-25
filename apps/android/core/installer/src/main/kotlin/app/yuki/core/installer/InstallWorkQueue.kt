package app.yuki.core.installer

import android.database.sqlite.SQLiteException
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.await
import app.yuki.core.model.InstallFailure
import java.time.Duration
import javax.inject.Inject

internal interface InstallWorkQueue {
    suspend fun enqueue(request: InstallRequest)

    fun cancel(githubRepoId: Long)
}

internal class WorkManagerInstallWorkQueue @Inject constructor(
    private val workManager: WorkManager,
) : InstallWorkQueue {
    override suspend fun enqueue(request: InstallRequest) {
        val githubRepoId = request.target.githubRepoId
        val operation = workManager.enqueueUniqueWork(
            installWorkName(githubRepoId),
            ExistingWorkPolicy.REPLACE,
            request.toWorkRequest(),
        )

        try {
            operation.await()
        } catch (error: SQLiteException) {
            throw InstallException(
                InstallFailure.ScheduleFailed,
                "WorkManager could not store the install for githubRepoId=$githubRepoId",
                error,
            )
        }
    }

    override fun cancel(githubRepoId: Long) {
        workManager.cancelUniqueWork(installWorkName(githubRepoId))
    }
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
