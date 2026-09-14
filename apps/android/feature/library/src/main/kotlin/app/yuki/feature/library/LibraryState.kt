package app.yuki.feature.library

import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.designsystem.component.installFailureLabel
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp

data class LibraryItem(
    val app: InstalledApp,
    val canOpen: Boolean,
    val install: InstallState,
) {
    val githubRepoId: Long get() = app.githubRepoId

    val isDownloading: Boolean get() = install is InstallState.Downloading

    val isFailed: Boolean get() = install is InstallState.Failed

    val isInstalled: Boolean get() = app.packageName.isNotEmpty()

    internal val sortRank: Int get() = when {
        isDownloading -> RANK_DOWNLOADING
        install == InstallState.Installing -> RANK_DOWNLOADING
        install == InstallState.PendingUserAction -> RANK_PENDING
        isFailed -> RANK_FAILED
        else -> RANK_SETTLED
    }

    val failure: InstallFailure? get() = (install as? InstallState.Failed)?.reason

    val listItem: ProductListItemContent get() = ProductListItemContent(
        title = app.title,
        supporting = supportingText(),
        iconUrl = app.iconUrl,
    )

    private fun supportingText(): String = when (install) {
        is InstallState.Downloading -> install.size.label
        InstallState.Installing -> LIBRARY_INSTALLING_SUPPORTING
        InstallState.PendingUserAction -> LIBRARY_PENDING_SUPPORTING
        is InstallState.Failed -> installFailureLabel(install.reason)
        else -> app.versionTag
    }
}

data class LibraryContent(
    val items: List<LibraryItem>,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    val downloading: List<LibraryItem> get() = items.filter(LibraryItem::isDownloading)
}

private const val RANK_DOWNLOADING = 0
private const val RANK_PENDING = 1
private const val RANK_FAILED = 2
private const val RANK_SETTLED = 3

internal const val LIBRARY_INSTALLING_SUPPORTING = "Installing"
internal const val LIBRARY_PENDING_SUPPORTING = "Waiting for confirmation"
