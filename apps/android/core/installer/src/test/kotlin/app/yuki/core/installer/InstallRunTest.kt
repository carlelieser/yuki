package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallRunTest {
    @Test
    fun `every state the install passes through is written to the store`() = runTest {
        val progress = FakeInstallProgressStore()
        val run = runOf(
            progress = progress,
            downloader = FakeApkDownloader(sizes = listOf(testSize(250L), testSize(750L))),
        )

        run.execute(testRequest())

        assertEquals(
            listOf(
                InstallState.Downloading(testSize(0L, bytesTotal = 0L)),
                InstallState.Downloading(testSize(250L)),
                InstallState.Downloading(testSize(750L)),
                InstallState.Installed("v1.2.0"),
            ),
            progress.written.map(InstallProgress::state),
        )
    }

    @Test
    fun `progress is written against the repo id so any screen can observe it`() = runTest {
        val progress = FakeInstallProgressStore()

        runOf(progress = progress).execute(testRequest(githubRepoId = 77L))

        assertTrue(progress.written.all { written -> written.githubRepoId == 77L })
        assertEquals(InstallState.Installed("v1.2.0"), progress.find(77L)?.state)
    }

    @Test
    fun `a queued install is observable before any bytes arrive`() = runTest {
        val progress = FakeInstallProgressStore()

        runOf(progress = progress).execute(testRequest())

        val queued = progress.written.first().state as InstallState.Downloading
        assertEquals(0L, queued.size.bytesDownloaded)
        assertEquals(null, queued.size.bytesTotal)
    }

    @Test
    fun `a failure is persisted so a restarted process can still show it`() = runTest {
        val progress = FakeInstallProgressStore()
        val run = runOf(
            progress = progress,
            downloader = FakeApkDownloader(
                failure = InstallException(InstallFailure.DownloadUnreadable, "truncated"),
            ),
        )

        val terminal = run.execute(testRequest())

        assertEquals(InstallState.Failed(InstallFailure.DownloadUnreadable), terminal)
        assertEquals(terminal, progress.find(42L)?.state)
    }
}

class InstallRetryPolicyTest {
    @Test
    fun `a successful install is never retried`() {
        assertEquals(false, isRetryable(InstallState.Installed("v1"), runAttemptCount = 0))
    }

    @Test
    fun `a transient download failure is retried while attempts remain`() {
        assertEquals(true, isRetryable(downloadFailed(), runAttemptCount = 0))
        assertEquals(true, isRetryable(downloadFailed(), runAttemptCount = 1))
    }

    @Test
    fun `retrying stops once the attempt budget is spent`() {
        assertEquals(false, isRetryable(downloadFailed(), runAttemptCount = INSTALL_MAX_ATTEMPTS))
    }

    @Test
    fun `failures the user must resolve are not retried`() {
        val permanent = listOf(
            InstallFailure.DownloadUnreadable,
            InstallFailure.InsufficientStorage,
            InstallFailure.Incompatible,
            InstallFailure.PackageMismatch,
            InstallFailure.Aborted,
        )

        permanent.forEach { reason ->
            assertEquals(false, isRetryable(InstallState.Failed(reason), runAttemptCount = 0))
        }
    }
}

private fun downloadFailed(): InstallState = InstallState.Failed(InstallFailure.DownloadFailed)

private fun runOf(
    progress: InstallProgressStore,
    downloader: ApkDownloader = FakeApkDownloader(),
): InstallRun = InstallRun(
    coordinator = InstallCoordinator(
        downloader = downloader,
        recorder = FakeInstallRecorder(mutableMapOf()),
        strategies = InstallStrategySelector(
            identityReader = FakeApkIdentityReader(ApkIdentity("com.acme.app", 12L)),
            fallback = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
            privileged = null,
        ),
    ),
    progress = progress,
)
