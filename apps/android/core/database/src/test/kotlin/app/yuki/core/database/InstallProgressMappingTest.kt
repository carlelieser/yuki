package app.yuki.core.database

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

private const val REPO_ID = 42L
private const val VERSION_TAG = "v1.2.0"
private const val UPDATED_AT = 1_700_000_000_000L
private val TARGET = InstallTarget(
    githubRepoId = REPO_ID,
    slug = "termux",
    title = "Termux",
    iconUrl = "https://example.test/termux.png",
)

class InstallProgressMappingTest {
    @Test
    fun `a download with a known total round trips its byte counts`() {
        val size = downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 12_100_000L)

        assertEquals(InstallState.Downloading(size), roundTrip(InstallState.Downloading(size)))
    }

    @Test
    fun `a download with an unknown total round trips as unknown rather than zero`() {
        val size = downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 0L)
        val restored = roundTrip(InstallState.Downloading(size)) as InstallState.Downloading

        assertEquals(null, restored.size.bytesTotal)
        assertEquals(null, restored.size.fraction)
        assertEquals(4_100_000L, restored.size.bytesDownloaded)
    }

    @Test
    fun `pending user action round trips`() {
        assertEquals(InstallState.PendingUserAction, roundTrip(InstallState.PendingUserAction))
    }

    @Test
    fun `an install in progress round trips`() {
        assertEquals(InstallState.Installing, roundTrip(InstallState.Installing))
    }

    @Test
    fun `an install in progress is persisted distinctly from a pending confirmation`() {
        val installing = InstallProgress(TARGET, VERSION_TAG, InstallState.Installing)
            .toEntity(UPDATED_AT)
        val pending = InstallProgress(TARGET, VERSION_TAG, InstallState.PendingUserAction)
            .toEntity(UPDATED_AT)

        assertNotEquals(installing.status, pending.status)
    }

    @Test
    fun `an installed version round trips its tag`() {
        assertEquals(
            InstallState.Installed(VERSION_TAG),
            roundTrip(InstallState.Installed(VERSION_TAG)),
        )
    }

    @Test
    fun `every failure reason round trips distinctly`() {
        val reasons = listOf(
            InstallFailure.DownloadFailed(httpStatus = null),
            InstallFailure.DownloadUnreadable,
            InstallFailure.NotAnApk,
            InstallFailure.Aborted,
            InstallFailure.InsufficientStorage,
            InstallFailure.Incompatible,
            InstallFailure.InvalidApk,
            InstallFailure.PackageMismatch,
            InstallFailure.SignatureConflict,
            InstallFailure.TimedOut,
        )

        reasons.forEach { reason ->
            assertEquals(InstallState.Failed(reason), roundTrip(InstallState.Failed(reason)))
        }
    }

    @Test
    fun `a download failure keeps its http status`() {
        val notFound = InstallState.Failed(InstallFailure.DownloadFailed(httpStatus = 404))

        assertEquals(notFound, roundTrip(notFound))
    }

    @Test
    fun `a rejection keeps its installer message`() {
        val rejected = InstallState.Failed(InstallFailure.Rejected("blocked by policy"))

        assertEquals(rejected, roundTrip(rejected))
    }

    @Test
    fun `an unreadable download is persisted separately from an incompatible package`() {
        val unreadable = InstallProgress(
            TARGET,
            VERSION_TAG,
            InstallState.Failed(InstallFailure.DownloadUnreadable),
        ).toEntity(UPDATED_AT)
        val incompatible = InstallProgress(
            TARGET,
            VERSION_TAG,
            InstallState.Failed(InstallFailure.Incompatible),
        ).toEntity(UPDATED_AT)

        assertEquals(false, unreadable.failureReason == incompatible.failureReason)
    }

    @Test
    fun `the persisted row carries the identity needed to render a library row`() {
        val entity = InstallProgress(TARGET, VERSION_TAG, InstallState.PendingUserAction)
            .toEntity(UPDATED_AT)

        assertEquals("termux", entity.slug)
        assertEquals("Termux", entity.title)
        assertEquals("https://example.test/termux.png", entity.iconUrl)
    }

    @Test
    fun `the install target round trips so a download can render before it is installed`() {
        val restored = InstallProgress(TARGET, VERSION_TAG, InstallState.PendingUserAction)
            .toEntity(UPDATED_AT)
            .toProgress()

        assertEquals(TARGET, restored.target)
    }

    @Test
    fun `the persisted row carries the repo id version tag and timestamp`() {
        val entity = InstallProgress(TARGET, VERSION_TAG, InstallState.PendingUserAction)
            .toEntity(UPDATED_AT)

        assertEquals(REPO_ID, entity.githubRepoId)
        assertEquals(VERSION_TAG, entity.versionTag)
        assertEquals(UPDATED_AT, entity.updatedAt)
    }

    @Test
    fun `a freshly mapped row stamps both timestamps`() {
        val entity = InstallProgress(TARGET, VERSION_TAG, InstallState.Installing)
            .toEntity(UPDATED_AT)

        assertEquals(UPDATED_AT, entity.createdAt)
        assertEquals(UPDATED_AT, entity.updatedAt)
    }
}

private fun roundTrip(state: InstallState): InstallState =
    InstallProgress(TARGET, VERSION_TAG, state).toEntity(UPDATED_AT).toProgress().state
