package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.SelfListing
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

internal const val INSTALL_MAX_ATTEMPTS = 3

internal class InstallRun @Inject constructor(
    private val coordinator: InstallCoordinator,
    private val progress: InstallProgressStore,
    private val self: SelfListing,
) {
    suspend fun execute(request: InstallRequest, runAttemptCount: Int): InstallState {
        if (self.isRunning(request)) return settleLanded(request)

        val target = request.target
        val versionTag = request.source.versionTag
        var latest: InstallState = QUEUED

        progress.write(InstallProgress(target, versionTag, latest))

        try {
            coordinator.install(request).collect { state ->
                latest = state
                val shown = if (isRetryable(state, runAttemptCount)) QUEUED else state
                progress.write(InstallProgress(target, versionTag, shown))
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            progress.write(InstallProgress(target, versionTag, UNEXPECTED))
            throw error
        }

        return latest
    }

    private suspend fun settleLanded(request: InstallRequest): InstallState {
        val landed = InstallState.Installed(request.source.versionTag)
        progress.write(InstallProgress(request.target, request.source.versionTag, landed))
        return landed
    }
}

internal fun SelfListing.isRunning(request: InstallRequest): Boolean =
    isRelease &&
        request.target.githubRepoId == githubRepoId &&
        request.source.versionTag == releaseTag

internal fun isRetryable(terminal: InstallState, runAttemptCount: Int): Boolean {
    val failure = (terminal as? InstallState.Failed)?.reason ?: return false
    val hasAttemptsLeft = runAttemptCount + 1 < INSTALL_MAX_ATTEMPTS

    return failure.isTransient() && hasAttemptsLeft
}

private const val FIRST_SERVER_ERROR = 500
private val TRANSIENT_CLIENT_ERRORS = setOf(408, 429)

private fun InstallFailure.isTransient(): Boolean {
    val download = this as? InstallFailure.DownloadFailed ?: return false
    val status = download.httpStatus ?: return true

    return status >= FIRST_SERVER_ERROR || status in TRANSIENT_CLIENT_ERRORS
}

private val UNEXPECTED = InstallState.Failed(InstallFailure.Unexpected)

private val QUEUED = InstallState.Queued
