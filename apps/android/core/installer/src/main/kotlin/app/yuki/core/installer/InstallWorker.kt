package app.yuki.core.installer

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
internal class InstallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val run: InstallRun,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val terminal = run.execute(inputData.toInstallRequest(), runAttemptCount)

        return if (isRetryable(terminal, runAttemptCount)) Result.retry() else Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        installForegroundInfo(applicationContext, inputData.toInstallRequest().target)
}
