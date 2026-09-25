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

        run.execute(testRequest(), runAttemptCount = 0)

        assertEquals(
            listOf(
                InstallState.Downloading(testSize(0L, bytesTotal = 0L)),
                InstallState.Downloading(testSize(250L)),
                InstallState.Downloading(testSize(750L)),
                InstallState.Installing,
                InstallState.Installed("v1.2.0"),
            ),
            progress.written.map(InstallProgress::state),
        )
    }

    @Test
    fun `progress is written against the repo id so any screen can observe it`() = runTest {
        val progress = FakeInstallProgressStore()

        runOf(progress = progress).execute(testRequest(githubRepoId = 77L), runAttemptCount = 0)

        assertTrue(progress.written.all { written -> written.githubRepoId == 77L })
        assertEquals(InstallState.Installed("v1.2.0"), progress.find(77L)?.state)
    }

    @Test
    fun `a queued install is observable before any bytes arrive`() = runTest {
        val progress = FakeInstallProgressStore()

        runOf(progress = progress).execute(testRequest(), runAttemptCount = 0)

        val queued = progress.written.first().state as InstallState.Downloading
        assertEquals(0L, queued.size.bytesDownloaded)
        assertEquals(null, queued.size.bytesTotal)
    }

    @Test
    fun `progress carries the identity a library row needs before anything is installed`() =
        runTest {
            val progress = FakeInstallProgressStore()

            runOf(progress = progress).execute(testRequest(githubRepoId = 77L), runAttemptCount = 0)

            val target = progress.find(77L)?.target
            assertEquals("acme-app", target?.slug)
            assertEquals("Acme App", target?.title)
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

        val terminal = run.execute(testRequest(), runAttemptCount = 0)

        assertEquals(InstallState.Failed(InstallFailure.DownloadUnreadable), terminal)
        assertEquals(terminal, progress.find(42L)?.state)
    }

    @Test
    fun `a failure that will be retried leaves the install queued`() = runTest {
        val progress = FakeInstallProgressStore()
        val run = runOf(progress = progress, downloader = failingDownloader(httpStatus = 503))

        val terminal = run.execute(testRequest(), runAttemptCount = 0)

        assertEquals(downloadFailedWith(503), terminal)
        assertEquals(QUEUED_STATE, progress.find(42L)?.state)
    }

    @Test
    fun `a failure on the last attempt is persisted`() = runTest {
        val progress = FakeInstallProgressStore()
        val run = runOf(progress = progress, downloader = failingDownloader(httpStatus = 503))

        run.execute(testRequest(), runAttemptCount = INSTALL_MAX_ATTEMPTS - 1)

        assertEquals(downloadFailedWith(503), progress.find(42L)?.state)
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
    fun `a server error is retried`() {
        assertEquals(true, isRetryable(downloadFailedWith(503), runAttemptCount = 0))
    }

    @Test
    fun `a missing release is not retried`() {
        assertEquals(false, isRetryable(downloadFailedWith(404), runAttemptCount = 0))
        assertEquals(false, isRetryable(downloadFailedWith(410), runAttemptCount = 0))
    }

    @Test
    fun `a timed out or throttled request is retried`() {
        assertEquals(true, isRetryable(downloadFailedWith(408), runAttemptCount = 0))
        assertEquals(true, isRetryable(downloadFailedWith(429), runAttemptCount = 0))
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
            InstallFailure.TimedOut,
        )

        permanent.forEach { reason ->
            assertEquals(false, isRetryable(InstallState.Failed(reason), runAttemptCount = 0))
        }
    }
}

private val QUEUED_STATE = InstallState.Downloading(testSize(0L, bytesTotal = 0L))

private fun failingDownloader(httpStatus: Int): FakeApkDownloader = FakeApkDownloader(
    failure = InstallException(InstallFailure.DownloadFailed(httpStatus), "server error"),
)

private fun downloadFailedWith(httpStatus: Int): InstallState =
    InstallState.Failed(InstallFailure.DownloadFailed(httpStatus))

private fun downloadFailed(): InstallState =
    InstallState.Failed(InstallFailure.DownloadFailed(httpStatus = null))

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
