package app.yuki.feature.library

import app.yuki.core.model.InstalledApp
import app.yuki.core.model.LibraryEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private fun installedApp(id: Long, slug: String) = InstalledApp(
    githubRepoId = id,
    packageName = "app.$slug",
    slug = slug,
    title = slug,
    iconUrl = null,
    versionTag = "v1",
)

private fun libraryEntry(id: Long, slug: String) = LibraryEntry(
    githubRepoId = id,
    packageName = "app.$slug",
    slug = slug,
    title = slug,
    iconUrl = null,
    versionTag = "v1",
)

private class NoopRefresh : DetectedInstallRefresh {
    override suspend fun reconcile() = Unit
}

private fun syncWith(
    installs: List<InstalledApp>,
    remote: FakeRemoteLibrary,
) = LibrarySync(
    detection = NoopRefresh(),
    store = FakeInstallStore(installs),
    remote = remote,
)

class LibrarySyncTest {
    @Test
    fun `an install the server has not seen is uploaded`() = runTest {
        val remote = FakeRemoteLibrary()
        val sync = syncWith(listOf(installedApp(1, "link-sheet")), remote)

        val result = sync.run()

        assertEquals(listOf("link-sheet"), remote.recorded)
        assertEquals(1, result.uploaded)
    }

    @Test
    fun `an install the server already holds is left alone`() = runTest {
        val remote = FakeRemoteLibrary(entries = listOf(libraryEntry(1, "link-sheet")))
        val sync = syncWith(listOf(installedApp(1, "link-sheet")), remote)

        val result = sync.run()

        assertEquals(emptyList<String>(), remote.recorded)
        assertEquals(0, result.uploaded)
    }

    @Test
    fun `a rejected upload does not stop the rest`() = runTest {
        val remote = FakeRemoteLibrary(failing = setOf("broken"))
        val sync = syncWith(
            listOf(installedApp(1, "broken"), installedApp(2, "link-sheet")),
            remote,
        )

        val result = sync.run()

        assertEquals(listOf("broken", "link-sheet"), remote.recorded)
        assertEquals(1, result.uploaded)
        assertEquals(1, result.failed)
    }
}
