package app.yuki.feature.library

import app.yuki.core.model.InstallState
import app.yuki.core.model.LibraryEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun item(slug: String, presence: LibraryPresence) = LibraryItem(
    entry = LibraryEntry(
        githubRepoId = slug.hashCode().toLong(),
        packageName = "com.$slug",
        slug = slug,
        title = slug,
        iconUrl = null,
        versionTag = "v1",
    ),
    presence = presence,
    install = InstallState.NotInstalled,
)

private val ON_DEVICE = item("aurora-store", LibraryPresence.Installed)
private val IN_LIBRARY_ONLY = item("obsidian", LibraryPresence.NotInstalled)

private fun content(filter: LibraryFilter) =
    LibraryContent(items = listOf(ON_DEVICE, IN_LIBRARY_ONLY), filter = filter)

class LibraryFilterTest {
    @Test
    fun `the default filter shows everything in the library`() {
        assertEquals(2, content(LibraryFilter.All).visible.size)
    }

    @Test
    fun `the installed filter hides entries that are not on the device`() {
        assertEquals(listOf("aurora-store"), content(LibraryFilter.Installed).visible.map(LibraryItem::slug))
    }

    @Test
    fun `the not-installed filter hides entries that are on the device`() {
        assertEquals(listOf("obsidian"), content(LibraryFilter.NotInstalled).visible.map(LibraryItem::slug))
    }

    @Test
    fun `a filter that matches nothing is told apart from an empty library`() {
        val filtered = LibraryContent(items = listOf(ON_DEVICE), filter = LibraryFilter.NotInstalled)

        assertFalse(filtered.isEmpty)
        assertTrue(filtered.hasNoMatches)
    }

    @Test
    fun `an empty library does not report a filter mismatch`() {
        val empty = LibraryContent(items = emptyList(), filter = LibraryFilter.Installed)

        assertTrue(empty.isEmpty)
        assertFalse(empty.hasNoMatches)
    }
}
