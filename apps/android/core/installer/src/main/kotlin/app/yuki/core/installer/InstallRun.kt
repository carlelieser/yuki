package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import javax.inject.Inject

internal const val INSTALL_MAX_ATTEMPTS = 3

internal class InstallRun @Inject constructor(
    private val coordinator: InstallCoordinator,
    private val progress: InstallProgressStore,
) {
    suspend fun execute(request: InstallRequest): InstallState {
        val githubRepoId = request.target.githubRepoId
        val versionTag = request.source.versionTag
        var latest: InstallState = QUEUED

        progress.write(InstallProgress(githubRepoId, versionTag, latest))

        coordinator.install(request).collect { state ->
            latest = state
            progress.write(InstallProgress(githubRepoId, versionTag, state))
        }

        return latest
    }
}

internal fun isRetryable(terminal: InstallState, runAttemptCount: Int): Boolean {
    val failure = (terminal as? InstallState.Failed)?.reason ?: return false
    val hasAttemptsLeft = runAttemptCount + 1 < INSTALL_MAX_ATTEMPTS

    return failure == InstallFailure.DownloadFailed && hasAttemptsLeft
}

private val QUEUED = InstallState.Downloading(downloadSizeOf(bytesDownloaded = 0L, bytesTotal = 0L))
