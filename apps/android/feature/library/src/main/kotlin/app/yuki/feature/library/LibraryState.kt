package app.yuki.feature.library

import app.yuki.core.model.DownloadSize
import app.yuki.core.model.InstallState
import app.yuki.core.model.LibraryEntry

enum class LibraryPresence { Installed, NotInstalled }

data class LibraryItem(
    val entry: LibraryEntry,
    val presence: LibraryPresence,
    val install: InstallState,
) {
    val githubRepoId: Long get() = entry.githubRepoId

    val slug: String get() = entry.slug

    val isDownloading: Boolean get() = install is InstallState.Downloading

    val isInstalled: Boolean get() = presence == LibraryPresence.Installed

    internal val sortRank: Int get() = when {
        isDownloading -> RANK_DOWNLOADING
        install == InstallState.Installing -> RANK_DOWNLOADING
        install == InstallState.PendingUserAction -> RANK_PENDING
        isInstalled -> RANK_INSTALLED
        else -> RANK_NOT_INSTALLED
    }

    val supporting: LibrarySupporting get() = when (install) {
        is InstallState.Downloading -> downloadSupporting(install)
        InstallState.Installing -> LibrarySupporting.Installing
        InstallState.PendingUserAction -> LibrarySupporting.Pending
        else -> settledSupporting()
    }

    private fun downloadSupporting(install: InstallState.Downloading): LibrarySupporting =
        if (install.size.isTotalKnown) LibrarySupporting.Download(install.size) else LibrarySupporting.None

    private fun settledSupporting(): LibrarySupporting = when {
        !isInstalled -> LibrarySupporting.NotInstalled
        entry.versionTag.isEmpty() -> LibrarySupporting.UnknownVersion
        else -> LibrarySupporting.Version(entry.versionTag)
    }
}

sealed interface LibrarySupporting {
    data class Version(val tag: String) : LibrarySupporting

    data object UnknownVersion : LibrarySupporting

    data object NotInstalled : LibrarySupporting

    data object Installing : LibrarySupporting

    data object Pending : LibrarySupporting

    data class Download(val size: DownloadSize) : LibrarySupporting

    data object None : LibrarySupporting
}

data class LibraryContent(
    val items: List<LibraryItem>,
    val filter: LibraryFilter = LibraryFilter.All,
) {
    val visible: List<LibraryItem> = items.filter(filter::accepts)

    val isEmpty: Boolean get() = items.isEmpty()

    val hasNoMatches: Boolean get() = visible.isEmpty() && items.isNotEmpty()

    val downloading: List<LibraryItem> get() = items.filter(LibraryItem::isDownloading)
}

private const val RANK_DOWNLOADING = 0
private const val RANK_PENDING = 1
private const val RANK_INSTALLED = 2
private const val RANK_NOT_INSTALLED = 3
