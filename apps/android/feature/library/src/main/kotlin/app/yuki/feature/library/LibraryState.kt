package app.yuki.feature.library

import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.model.InstallFailure
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

    val isFailed: Boolean get() = install is InstallState.Failed

    val isInstalled: Boolean get() = presence == LibraryPresence.Installed

    internal val sortRank: Int get() = when {
        isDownloading -> RANK_DOWNLOADING
        install == InstallState.Installing -> RANK_DOWNLOADING
        install == InstallState.PendingUserAction -> RANK_PENDING
        isFailed -> RANK_FAILED
        isInstalled -> RANK_INSTALLED
        else -> RANK_NOT_INSTALLED
    }

    val failure: InstallFailure? get() = (install as? InstallState.Failed)?.reason

    val listItem: ProductListItemContent get() = ProductListItemContent(
        title = entry.title,
        supporting = supportingText(),
        iconUrl = entry.iconUrl,
        installState = install,
    )

    private fun supportingText(): String = when (install) {
        is InstallState.Downloading -> if (install.size.isTotalKnown) install.size.label else ""
        InstallState.Installing -> LIBRARY_INSTALLING_SUPPORTING
        InstallState.PendingUserAction -> LIBRARY_PENDING_SUPPORTING
        else -> settledSupportingText()
    }

    private fun settledSupportingText(): String = when {
        !isInstalled -> LIBRARY_NOT_INSTALLED_SUPPORTING
        else -> entry.versionTag.ifEmpty { LIBRARY_UNKNOWN_VERSION_SUPPORTING }
    }
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
private const val RANK_FAILED = 2
private const val RANK_INSTALLED = 3
private const val RANK_NOT_INSTALLED = 4

internal const val LIBRARY_INSTALLING_SUPPORTING = "Installing"
internal const val LIBRARY_PENDING_SUPPORTING = "Waiting for confirmation"
internal const val LIBRARY_UNKNOWN_VERSION_SUPPORTING = "Version unknown"
internal const val LIBRARY_NOT_INSTALLED_SUPPORTING = "Not installed"
