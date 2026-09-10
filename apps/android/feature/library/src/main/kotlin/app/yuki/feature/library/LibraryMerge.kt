package app.yuki.feature.library

import app.yuki.core.installer.InstallProgress
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp

internal data class LibraryMergeInput(
    val installed: List<InstalledApp>,
    val active: List<InstallProgress>,
)

internal fun mergeLibrary(
    input: LibraryMergeInput,
    canOpen: (String) -> Boolean,
): List<LibraryItem> {
    val progressByRepoId = input.active.associateBy(InstallProgress::githubRepoId)
    val installedRows = input.installed.map { app ->
        LibraryItem(
            app = app,
            canOpen = canOpen(app.packageName),
            install = progressByRepoId[app.githubRepoId]?.state ?: InstallState.NotInstalled,
        )
    }
    val installedRepoIds = input.installed.mapTo(mutableSetOf(), InstalledApp::githubRepoId)
    val pendingRows = input.active
        .filterNot { progress -> progress.githubRepoId in installedRepoIds }
        .filter { progress -> progress.isVisibleWhileUninstalled }
        .map(InstallProgress::toPendingItem)

    return (pendingRows + installedRows).sortedBy { item -> item.sortRank }
}

private val InstallProgress.isVisibleWhileUninstalled: Boolean
    get() = state !is InstallState.Installed

private fun InstallProgress.toPendingItem(): LibraryItem = LibraryItem(
    app = InstalledApp(
        githubRepoId = target.githubRepoId,
        packageName = "",
        slug = target.slug,
        title = target.title,
        iconUrl = target.iconUrl,
        versionTag = versionTag,
    ),
    canOpen = false,
    install = state,
)
