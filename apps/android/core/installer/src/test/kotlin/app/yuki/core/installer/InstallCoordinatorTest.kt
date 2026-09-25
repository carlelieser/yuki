package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val ACME_PACKAGE = "com.acme.app"

private val ACME_IDENTITY = ApkIdentity(ACME_PACKAGE, versionCode = 12L)

class InstallCoordinatorTest {
    @Test
    fun `first install emits progress then installed and records identity`() = runTest {
        val recorder = FakeInstallRecorder(mutableMapOf())
        val coordinator = coordinatorOf(
            downloader = FakeApkDownloader(sizes = listOf(testSize(250L), testSize(750L))),
            recorder = recorder,
            strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
        )

        val states = coordinator.install(testRequest()).toList()

        assertEquals(
            listOf(
                InstallState.Downloading(testSize(250L)),
                InstallState.Downloading(testSize(750L)),
                InstallState.Installing,
                InstallState.Installed("v1.2.0"),
            ),
            states,
        )
        assertEquals(listOf(ACME_PACKAGE), recorder.records.map { it.identity.packageName })
        assertEquals(12L, recorder.records.single().identity.versionCode)
    }

    @Test
    fun `pending user action surfaces before success`() = runTest {
        val coordinator = coordinatorOf(
            strategy = FakeInstallStrategy(
                listOf(InstallOutcome.AwaitingUserAction, InstallOutcome.Succeeded),
            ),
        )

        val states = coordinator.install(testRequest()).toList()

        assertEquals(
            listOf(
                InstallState.Downloading(testSize(500L)),
                InstallState.Installing,
                InstallState.PendingUserAction,
                InstallState.Installed("v1.2.0"),
            ),
            states,
        )
    }

    @Test
    fun `update to the same package is allowed`() = runTest {
        val recorder = FakeInstallRecorder(mutableMapOf(42L to ACME_PACKAGE))
        val coordinator = coordinatorOf(
            recorder = recorder,
            strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
        )

        val states = coordinator.install(testRequest(versionTag = "v2.0.0")).toList()

        assertEquals(InstallState.Installed("v2.0.0"), states.last())
        assertEquals(1, recorder.records.size)
    }

    @Test
    fun `update whose apk changed package name is refused`() = runTest {
        val recorder = FakeInstallRecorder(mutableMapOf(42L to "com.acme.original"))
        val strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))
        val coordinator = coordinatorOf(recorder = recorder, strategy = strategy)

        val states = coordinator.install(testRequest(versionTag = "v2.0.0")).toList()

        assertEquals(InstallState.Failed(InstallFailure.PackageMismatch), states.last())
        assertEquals(0, strategy.installCount)
        assertTrue(recorder.records.isEmpty())
    }

    @Test
    fun `a package mismatch never reports an install in progress`() = runTest {
        val recorder = FakeInstallRecorder(mutableMapOf(42L to "com.acme.original"))
        val coordinator = coordinatorOf(
            recorder = recorder,
            strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
        )

        val states = coordinator.install(testRequest(versionTag = "v2.0.0")).toList()

        assertTrue(InstallState.Installing !in states)
    }

    @Test
    fun `the install phase is announced before the strategy runs`() = runTest {
        val strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))
        val coordinator = coordinatorOf(strategy = strategy)

        val states = coordinator.install(testRequest()).toList()

        val installing = states.indexOf(InstallState.Installing)
        val installed = states.indexOf(InstallState.Installed("v1.2.0"))

        assertTrue(installing in 0..<installed)
    }

    @Test
    fun `a download failure never reports an install in progress`() = runTest {
        val coordinator = coordinatorOf(
            downloader = FakeApkDownloader(
                failure = InstallException(DOWNLOAD_FAILED, "boom"),
            ),
            strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
        )

        val states = coordinator.install(testRequest()).toList()

        assertTrue(InstallState.Installing !in states)
    }

    @Test
    fun `download failure surfaces as a typed failure and never installs`() = runTest {
        val strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))
        val coordinator = coordinatorOf(
            downloader = FakeApkDownloader(
                failure = InstallException(DOWNLOAD_FAILED, "boom"),
            ),
            strategy = strategy,
        )

        val states = coordinator.install(testRequest()).toList()

        assertEquals(listOf(InstallState.Failed(DOWNLOAD_FAILED)), states)
        assertEquals(0, strategy.installCount)
    }

    @Test(expected = CancellationException::class)
    fun `cancellation propagates instead of becoming a failure state`() = runTest {
        val coordinator = coordinatorOf(
            downloader = FakeApkDownloader(failure = null, cancelMidway = true),
        )

        coordinator.install(testRequest()).toList()
    }

    @Test
    fun `an unexpected error propagates instead of posing as a rejection`() = runTest {
        val coordinator = InstallCoordinator(
            downloader = FakeApkDownloader(failure = IllegalStateException("bug")),
            recorder = FakeInstallRecorder(mutableMapOf()),
            strategies = InstallStrategySelector(
                identityReader = FakeApkIdentityReader(ApkIdentity("com.termux", 1L)),
                fallback = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
                privileged = null,
            ),
        )

        val error = runCatching { coordinator.install(testRequest()).toList() }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
    }

    @Test
    fun `an install that could not be saved is reported as not recorded`() = runTest {
        val coordinator = InstallCoordinator(
            downloader = FakeApkDownloader(),
            recorder = RefusingInstallRecorder(),
            strategies = InstallStrategySelector(
                identityReader = FakeApkIdentityReader(ApkIdentity("com.termux", 1L)),
                fallback = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
                privileged = null,
            ),
        )

        val states = coordinator.install(testRequest()).toList()

        assertEquals(InstallState.Failed(InstallFailure.NotRecorded), states.last())
        assertTrue(states.none { state -> state is InstallState.Installed })
    }

    @Test
    fun `strategy failure is surfaced and nothing is recorded`() = runTest {
        val recorder = FakeInstallRecorder(mutableMapOf())
        val coordinator = coordinatorOf(
            recorder = recorder,
            strategy = FakeInstallStrategy(
                listOf(InstallOutcome.Failed(InstallFailure.InsufficientStorage)),
            ),
        )

        val states = coordinator.install(testRequest()).toList()

        assertEquals(InstallState.Failed(InstallFailure.InsufficientStorage), states.last())
        assertTrue(recorder.records.isEmpty())
    }
}

private fun coordinatorOf(
    downloader: ApkDownloader = FakeApkDownloader(),
    recorder: InstallRecorder = FakeInstallRecorder(mutableMapOf()),
    strategy: InstallStrategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
): InstallCoordinator = InstallCoordinator(
    downloader = downloader,
    recorder = recorder,
    strategies = InstallStrategySelector(
        identityReader = FakeApkIdentityReader(ACME_IDENTITY),
        fallback = strategy,
        privileged = null,
    ),
)

private val DOWNLOAD_FAILED = InstallFailure.DownloadFailed(httpStatus = null)

private class RefusingInstallRecorder : InstallRecorder {
    override suspend fun recordedPackageName(githubRepoId: Long): String? = null

    override suspend fun record(record: InstallRecord) =
        throw InstallException(InstallFailure.NotRecorded, "database unavailable")
}
