package app.yuki.feature.library

import app.yuki.core.installer.InstallProgress
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.LibraryEntry

internal data class LibraryMergeInput(
    val installed: List<InstalledApp>,
    val remote: List<LibraryEntry>,
    val active: List<InstallProgress>,
)

internal fun mergeLibrary(input: LibraryMergeInput): List<LibraryItem> {
    val rows = linkedMapOf<Long, LibraryItem>()

    input.remote.forEach { entry -> rows[entry.githubRepoId] = entry.toRemoteItem() }
    input.installed.forEach { app -> rows[app.githubRepoId] = app.toDeviceItem() }
    input.active.forEach { progress -> rows.applyProgress(progress) }

    return rows.values.sortedBy(LibraryItem::sortRank)
}

private fun MutableMap<Long, LibraryItem>.applyProgress(progress: InstallProgress) {
    val existing = this[progress.githubRepoId]

    if (existing != null) {
        this[progress.githubRepoId] = existing.copy(install = progress.state)
        return
    }

    if (progress.state is InstallState.Installed) return

    this[progress.githubRepoId] = progress.toPendingItem()
}

private fun LibraryEntry.toRemoteItem(): LibraryItem = LibraryItem(
    entry = this,
    presence = LibraryPresence.NotInstalled,
    install = InstallState.NotInstalled,
)

private fun InstalledApp.toDeviceItem(): LibraryItem = LibraryItem(
    entry = LibraryEntry(
        githubRepoId = githubRepoId,
        packageName = packageName,
        slug = slug,
        title = title,
        iconUrl = iconUrl,
        versionTag = versionTag,
    ),
    presence = LibraryPresence.Installed,
    install = InstallState.NotInstalled,
)

private fun InstallProgress.toPendingItem(): LibraryItem = LibraryItem(
    entry = LibraryEntry(
        githubRepoId = target.githubRepoId,
        packageName = null,
        slug = target.slug,
        title = target.title,
        iconUrl = target.iconUrl,
        versionTag = versionTag,
    ),
    presence = LibraryPresence.NotInstalled,
    install = state,
)
