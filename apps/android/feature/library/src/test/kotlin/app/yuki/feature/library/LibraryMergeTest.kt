package app.yuki.feature.library

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.LibraryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

private const val AURORA_REPO_ID = 42L

private val AURORA_ON_DEVICE = InstalledApp(
    githubRepoId = AURORA_REPO_ID,
    packageName = "com.aurora.store",
    slug = "aurora-store",
    title = "Aurora Store",
    iconUrl = null,
    versionTag = "v1.2.0",
)

private val AURORA_IN_LIBRARY = LibraryEntry(
    githubRepoId = AURORA_REPO_ID,
    packageName = "com.aurora.store",
    slug = "aurora-store",
    title = "Aurora Store",
    iconUrl = null,
    versionTag = "v1.2.0",
)

private val OBSIDIAN_IN_LIBRARY = LibraryEntry(
    githubRepoId = 99L,
    packageName = "md.obsidian",
    slug = "obsidian",
    title = "Obsidian",
    iconUrl = null,
    versionTag = "v1.5.0",
)

private fun merge(
    installed: List<InstalledApp> = emptyList(),
    remote: List<LibraryEntry> = emptyList(),
    active: List<InstallProgress> = emptyList(),
) = mergeLibrary(LibraryMergeInput(installed, remote, active))

class LibraryMergeTest {
    @Test
    fun `a library entry that is not on the device is listed as not installed`() {
        val items = merge(remote = listOf(OBSIDIAN_IN_LIBRARY))

        assertEquals(listOf("obsidian"), items.map(LibraryItem::slug))
        assertEquals(listOf(LibraryPresence.NotInstalled), items.map(LibraryItem::presence))
    }

    @Test
    fun `an app on the device and in the library is listed once as installed`() {
        val items = merge(
            installed = listOf(AURORA_ON_DEVICE),
            remote = listOf(AURORA_IN_LIBRARY),
        )

        assertEquals(1, items.size)
        assertEquals(LibraryPresence.Installed, items.single().presence)
    }

    @Test
    fun `an app on the device but not in the library still appears`() {
        val items = merge(installed = listOf(AURORA_ON_DEVICE))

        assertEquals(listOf("aurora-store"), items.map(LibraryItem::slug))
        assertEquals(LibraryPresence.Installed, items.single().presence)
    }

    @Test
    fun `progress for a library entry does not add a second row`() {
        val items = merge(
            remote = listOf(AURORA_IN_LIBRARY),
            active = listOf(downloading(AURORA_REPO_ID, "aurora-store")),
        )

        assertEquals(1, items.size)
        assertEquals(InstallState.Installing, items.single().install)
    }

    @Test
    fun `installed rows sort ahead of library rows that are not installed`() {
        val items = merge(
            installed = listOf(AURORA_ON_DEVICE),
            remote = listOf(OBSIDIAN_IN_LIBRARY, AURORA_IN_LIBRARY),
        )

        assertEquals(listOf("aurora-store", "obsidian"), items.map(LibraryItem::slug))
    }

    @Test
    fun `a download in progress sorts ahead of everything else`() {
        val items = merge(
            installed = listOf(AURORA_ON_DEVICE),
            remote = listOf(OBSIDIAN_IN_LIBRARY),
            active = listOf(downloading(OBSIDIAN_IN_LIBRARY.githubRepoId, "obsidian")),
        )

        assertEquals("obsidian", items.first().slug)
    }

    @Test
    fun `a queued install waits at the top with the downloads`() {
        val queued = InstallProgress(
            target = InstallTarget(99L, "obsidian", "Obsidian", iconUrl = null),
            versionTag = "v1.5.0",
            state = InstallState.Queued,
        )

        val rows = merge(installed = listOf(AURORA_ON_DEVICE), active = listOf(queued))

        assertEquals(listOf(99L, AURORA_REPO_ID), rows.map(LibraryItem::githubRepoId))
        assertEquals(LibrarySupporting.Queued, rows.first().supporting)
    }
}

private fun downloading(githubRepoId: Long, slug: String) = InstallProgress(
    target = InstallTarget(
        githubRepoId = githubRepoId,
        slug = slug,
        title = slug,
        iconUrl = null,
    ),
    versionTag = "v1.0.0",
    state = InstallState.Installing,
)
