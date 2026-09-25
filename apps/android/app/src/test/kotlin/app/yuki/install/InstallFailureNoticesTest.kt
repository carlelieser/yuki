package app.yuki.install

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstallRequest
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallFailureNoticesTest {
    private val store = ListProgressStore()
    private val restarted = mutableListOf<InstallRequest>()
    private val notices = InstallFailureNotices(store, restarted::add, BASE_URL)

    @Test
    fun `the oldest failure is the one shown`() = runTest {
        store.rows.value = listOf(downloading(1L), failed(2L), failed(3L))

        assertEquals(2L, notices.oldestFailure().first()?.githubRepoId)
    }

    @Test
    fun `nothing is shown while no install has failed`() = runTest {
        store.rows.value = listOf(downloading(1L))

        assertNull(notices.oldestFailure().first())
    }

    @Test
    fun `dismissing clears the failure so it is shown once`() = runTest {
        store.rows.value = listOf(failed(2L), failed(3L))

        notices.dismiss(failed(2L))

        assertEquals(3L, notices.oldestFailure().first()?.githubRepoId)
    }

    @Test
    fun `retrying clears the failure and starts the same version again`() = runTest {
        store.rows.value = listOf(failed(2L))

        notices.retry(failed(2L))

        assertTrue(store.rows.value.isEmpty())
        assertEquals(target(2L), restarted.single().target)
        assertEquals("v2", restarted.single().source.versionTag)
    }

    @Test
    fun `a retry downloads the stored version from the yuki server`() {
        val request = failed(2L).toRetryRequest(BASE_URL)

        assertTrue(request.source.downloadUrl.startsWith("$BASE_URL/listings/app-2/download/v2"))
        assertNull(request.source.assetName)
    }
}

private const val BASE_URL = "https://yuki.test"

private fun target(githubRepoId: Long) = InstallTarget(
    githubRepoId = githubRepoId,
    slug = "app-$githubRepoId",
    title = "App $githubRepoId",
    iconUrl = null,
)

private fun failed(githubRepoId: Long) = InstallProgress(
    target = target(githubRepoId),
    versionTag = "v2",
    state = InstallState.Failed(InstallFailure.DownloadFailed(httpStatus = null)),
)

private fun downloading(githubRepoId: Long) = InstallProgress(
    target = target(githubRepoId),
    versionTag = "v2",
    state = InstallState.Downloading(downloadSizeOf(bytesDownloaded = 0L, bytesTotal = 0L)),
)

private class ListProgressStore : InstallProgressStore {
    val rows = MutableStateFlow<List<InstallProgress>>(emptyList())

    override fun observe(githubRepoId: Long): Flow<InstallProgress?> =
        rows.map { current -> current.firstOrNull { row -> row.githubRepoId == githubRepoId } }

    override fun observeActive(): Flow<List<InstallProgress>> = rows

    override suspend fun find(githubRepoId: Long): InstallProgress? =
        rows.value.firstOrNull { row -> row.githubRepoId == githubRepoId }

    override suspend fun unsettled(): List<InstallProgress> = emptyList()

    override suspend fun write(progress: InstallProgress) {
        rows.value = rows.value + progress
    }

    override suspend fun clear(githubRepoId: Long) {
        rows.value = rows.value.filterNot { row -> row.githubRepoId == githubRepoId }
    }

    override suspend fun clearSettled() = Unit
}
