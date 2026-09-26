package app.yuki.feature.updates

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallState
import app.yuki.core.model.SelfListing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SELF_REPO_ID = 1_359_590_051L
private const val RUNNING_TAG = "android-v1.7.1"

private fun selfListing(releaseTag: String) = SelfListing(
    githubRepoId = SELF_REPO_ID,
    slug = "carlelieser-yuki",
    packageName = "app.yuki",
    title = "Yuki",
    iconUrl = "https://example.com/icon.png",
    releaseTag = releaseTag,
    versionCode = 318L,
)

private fun pending(githubRepoId: Long, versionTag: String) = InstallProgress(
    target = InstallTarget(githubRepoId, "carlelieser-yuki", "Yuki", iconUrl = null),
    versionTag = versionTag,
    state = InstallState.Installing,
)

class SelfUpdateSettlerTest {
    private val installer = RecordingUpdateInstaller()

    private fun settler(progress: InstallProgressStore, releaseTag: String = RUNNING_TAG) =
        SelfUpdateSettler(selfListing(releaseTag), progress, installer)

    @Test
    fun `an update to the running build is cancelled because it already landed`() = runTest {
        val progress = MapInstallProgressStore(pending(SELF_REPO_ID, RUNNING_TAG))

        val settled = settler(progress).settle()

        assertTrue(settled)
        assertEquals(listOf(SELF_REPO_ID), installer.cancelled)
    }

    @Test
    fun `an update to a newer build is left to finish`() = runTest {
        val progress = MapInstallProgressStore(pending(SELF_REPO_ID, "android-v1.8.0"))

        val settled = settler(progress).settle()

        assertFalse(settled)
        assertTrue(installer.cancelled.isEmpty())
    }

    @Test
    fun `installs of other apps are left alone`() = runTest {
        val progress = MapInstallProgressStore(pending(42L, RUNNING_TAG))

        settler(progress).settle()

        assertTrue(installer.cancelled.isEmpty())
    }

    @Test
    fun `a build that came from no release settles nothing`() = runTest {
        val progress = MapInstallProgressStore(pending(SELF_REPO_ID, ""))

        val settled = settler(progress, releaseTag = "").settle()

        assertFalse(settled)
        assertTrue(installer.cancelled.isEmpty())
    }
}

private class MapInstallProgressStore(vararg initial: InstallProgress) : InstallProgressStore {
    private val rows = MutableStateFlow(initial.associateBy(InstallProgress::githubRepoId))

    override fun observe(githubRepoId: Long): Flow<InstallProgress?> =
        rows.map { current -> current[githubRepoId] }

    override fun observeActive(): Flow<List<InstallProgress>> =
        rows.map { current -> current.values.toList() }

    override suspend fun find(githubRepoId: Long): InstallProgress? = rows.value[githubRepoId]

    override suspend fun unsettled(): List<InstallProgress> = rows.value.values.toList()

    override suspend fun write(progress: InstallProgress) {
        rows.value = rows.value + (progress.githubRepoId to progress)
    }

    override suspend fun clear(githubRepoId: Long) {
        rows.value = rows.value - githubRepoId
    }

    override suspend fun clearSettled() = Unit
}
