package app.yuki.feature.updates

import app.cash.turbine.test
import app.yuki.core.database.InstallRecording
import app.yuki.core.database.PendingUpdate
import app.yuki.core.model.InstalledApp
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class OutstandingUpdatesTest {
    private val installs = FakeInstallStore(listOf(TERMUX, AURORA))
    private val pending = FakePendingUpdateStore(
        listOf(
            PendingUpdate(TERMUX.githubRepoId, "v0.119.0", isNotified = false),
            PendingUpdate(AURORA.githubRepoId, "4.7.0", isNotified = true),
        ),
    )
    private val outstanding = OutstandingUpdates(installs, pending)

    @Test
    fun countsEveryInstalledAppWithANewerPendingVersion() = runTest {
        outstanding.observe().test {
            assertEquals(listOf(TERMUX, AURORA), awaitItem())
        }
    }

    @Test
    fun installingThePendingVersionClearsIt() = runTest {
        outstanding.observe().test {
            awaitItem()

            installs.record(recordingOf(TERMUX.copy(versionTag = "v0.119.0")))

            assertEquals(listOf(AURORA), awaitItem())
        }
    }

    @Test
    fun uninstallingAnAppClearsItsPendingUpdate() = runTest {
        outstanding.observe().test {
            awaitItem()

            installs.forget(AURORA.githubRepoId)

            assertEquals(listOf(TERMUX), awaitItem())
        }
    }

    @Test
    fun aPendingUpdateForAnAppThatIsNotInstalledIsIgnored() = runTest {
        val orphan = PendingUpdate(githubRepoId = 9L, versionTag = "v2", isNotified = false)

        assertEquals(emptyList<Any>(), outstandingUpdates(listOf(orphan), listOf(TERMUX)))
    }
}

private val TERMUX = installedApp(1_234L, "termux", "v0.118.0")

private val AURORA = installedApp(5_678L, "aurora-store", "4.6.4")

private fun recordingOf(app: InstalledApp): InstallRecording =
    InstallRecording(app = app, versionCode = null, installedAt = Instant.EPOCH)
