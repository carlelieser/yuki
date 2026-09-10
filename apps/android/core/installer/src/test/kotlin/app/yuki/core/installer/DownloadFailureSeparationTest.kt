package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DownloadFailureSeparationTest {
    @Test
    fun `an unreadable download reports itself as such and not as an incompatible package`() =
        runTest {
            val states = coordinatorRejecting(InstallFailure.DownloadUnreadable)
                .install(testRequest())
                .toList()

            assertEquals(InstallState.Failed(InstallFailure.DownloadUnreadable), states.last())
            assertNotEquals(InstallState.Failed(InstallFailure.Incompatible), states.last())
        }

    @Test
    fun `a genuinely rejected package still reports incompatible`() = runTest {
        val states = coordinatorRejecting(InstallFailure.Incompatible)
            .install(testRequest())
            .toList()

        assertEquals(InstallState.Failed(InstallFailure.Incompatible), states.last())
    }

    @Test
    fun `an unreadable download never reaches the install strategy`() = runTest {
        val strategy = FakeInstallStrategy(listOf(InstallOutcome.Succeeded))
        val coordinator = InstallCoordinator(
            downloader = FakeApkDownloader(),
            recorder = FakeInstallRecorder(mutableMapOf()),
            strategies = InstallStrategySelector(
                identityReader = ThrowingApkIdentityReader(InstallFailure.DownloadUnreadable),
                fallback = strategy,
                privileged = null,
            ),
        )

        coordinator.install(testRequest()).toList()

        assertEquals(0, strategy.installCount)
    }
}

class DownloadedApkTest {
    @Test
    fun `a download with no reported local file is an unreadable download`() {
        assertEquals(InstallFailure.DownloadUnreadable, failureOf(localPath = null).failure)
    }

    @Test
    fun `a reported path with no file behind it is an unreadable download`() {
        val missing = "/tmp/yuki-does-not-exist-${System.nanoTime()}.apk"

        assertEquals(InstallFailure.DownloadUnreadable, failureOf(missing).failure)
    }

    @Test
    fun `an empty downloaded file is an unreadable download`() {
        val empty = File.createTempFile("yuki-empty", ".apk").apply { deleteOnExit() }

        assertEquals(InstallFailure.DownloadUnreadable, failureOf(empty.absolutePath).failure)
    }

    @Test
    fun `the file DownloadManager reports is the file that gets staged`() {
        val written = writtenApk()

        assertEquals(
            written.absolutePath,
            verifyDownloadedApk(written.absolutePath, TEST_SOURCE).absolutePath,
        )
    }

    @Test
    fun `the staged path is the reported one and not one rebuilt from the asset name`() {
        val written = writtenApk()

        val resolved = verifyDownloadedApk(written.absolutePath, TEST_SOURCE)

        assertNotEquals(TEST_SOURCE.fileName(), resolved.name)
        assertEquals(true, resolved.isFile)
    }
}

private fun writtenApk(): File = File.createTempFile("yuki-written", ".apk").apply {
    writeBytes(ByteArray(16) { 1 })
    deleteOnExit()
}

private val TEST_SOURCE = InstallSource(
    downloadUrl = "https://example.test/download",
    versionTag = "v1.2.0",
    assetName = "app-release-universal.apk",
)

private fun failureOf(localPath: String?): InstallException = runCatching {
    verifyDownloadedApk(localPath, TEST_SOURCE)
}.exceptionOrNull() as InstallException

private class ThrowingApkIdentityReader(private val failure: InstallFailure) : ApkIdentityReader {
    override fun read(apk: File): ApkIdentity =
        throw InstallException(failure, "identity unavailable for ${apk.absolutePath}")
}

private fun coordinatorRejecting(failure: InstallFailure): InstallCoordinator = InstallCoordinator(
    downloader = FakeApkDownloader(),
    recorder = FakeInstallRecorder(mutableMapOf()),
    strategies = InstallStrategySelector(
        identityReader = ThrowingApkIdentityReader(failure),
        fallback = FakeInstallStrategy(listOf(InstallOutcome.Succeeded)),
        privileged = null,
    ),
)
