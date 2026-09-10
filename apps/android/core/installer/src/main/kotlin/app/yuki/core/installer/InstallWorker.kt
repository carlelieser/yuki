package app.yuki.core.installer

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

internal const val INSTALL_MAX_ATTEMPTS = 3

@HiltWorker
internal class InstallWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted parameters: WorkerParameters,
    private val coordinator: InstallCoordinator,
    private val progress: InstallProgressStore,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val request = inputData.toInstallRequest()
        val terminal = runInstall(request)

        return if (isRetryable(terminal)) Result.retry() else Result.success()
    }

    private suspend fun runInstall(request: InstallRequest): InstallState {
        val githubRepoId = request.target.githubRepoId
        val versionTag = request.source.versionTag
        var latest: InstallState = InstallState.Downloading(downloadSizeOf(0L, 0L))

        progress.write(InstallProgress(githubRepoId, versionTag, latest))

        coordinator.install(request).collect { state ->
            latest = state
            progress.write(InstallProgress(githubRepoId, versionTag, state))
        }

        return latest
    }

    private fun isRetryable(terminal: InstallState): Boolean {
        val failure = (terminal as? InstallState.Failed)?.reason ?: return false

        return failure == InstallFailure.DownloadFailed && runAttemptCount + 1 < INSTALL_MAX_ATTEMPTS
    }
}
