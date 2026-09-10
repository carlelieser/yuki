package app.yuki.core.installer

import app.yuki.core.model.InstallState
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadDiscardOrderingTest {
    @Test
    fun theApkIsStillAvailableWhileTheStrategyInstallsIt() = runTest {
        val downloader = FakeApkDownloader()
        val strategy = DiscardObservingStrategy(downloader)
        val coordinator = coordinatorWith(downloader, strategy)

        coordinator.install(testRequest()).toList()

        assertTrue(
            "the download was discarded before the strategy read it",
            strategy.wasApkPresentDuringInstall,
        )
    }

    @Test
    fun theDownloadIsDiscardedOnceTheInstallFinishes() = runTest {
        val downloader = FakeApkDownloader()
        val coordinator = coordinatorWith(downloader, SucceedingStrategy())

        coordinator.install(testRequest()).toList()

        assertEquals(listOf(TEST_APK), downloader.discarded)
    }

    @Test
    fun theDownloadIsDiscardedWhenTheInstallFails() = runTest {
        val downloader = FakeApkDownloader()
        val coordinator = coordinatorWith(downloader, FailingStrategy())

        val states = coordinator.install(testRequest()).toList()

        assertEquals(listOf(TEST_APK), downloader.discarded)
        assertTrue(states.any { state -> state is InstallState.Failed })
    }
}

private fun coordinatorWith(
    downloader: FakeApkDownloader,
    strategy: InstallStrategy,
): InstallCoordinator = InstallCoordinator(
    downloader = downloader,
    recorder = FakeInstallRecorder(mutableMapOf()),
    strategies = InstallStrategySelector(
        identityReader = FakeApkIdentityReader(TEST_IDENTITY),
        fallback = strategy,
        privileged = null,
    ),
)

private val TEST_IDENTITY = ApkIdentity("app.acme", 12L)

private class DiscardObservingStrategy(
    private val downloader: FakeApkDownloader,
) : InstallStrategy {
    var wasApkPresentDuringInstall: Boolean = false
        private set

    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        wasApkPresentDuringInstall = downloader.discarded.isEmpty()
        emit(InstallOutcome.Succeeded)
    }
}

private class SucceedingStrategy : InstallStrategy {
    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        emit(InstallOutcome.Succeeded)
    }
}

private class FailingStrategy : InstallStrategy {
    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        emit(InstallOutcome.Failed(app.yuki.core.model.InstallFailure.Aborted))
    }
}
