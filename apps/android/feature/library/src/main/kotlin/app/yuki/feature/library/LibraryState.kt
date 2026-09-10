package app.yuki.feature.library

import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp

data class LibraryItem(
    val app: InstalledApp,
    val canOpen: Boolean,
    val install: InstallState,
) {
    val githubRepoId: Long get() = app.githubRepoId

    val isDownloading: Boolean get() = install is InstallState.Downloading

    val listItem: ProductListItemContent get() = ProductListItemContent(
        title = app.title,
        supporting = supportingText(),
        iconUrl = app.iconUrl,
    )

    private fun supportingText(): String = when (install) {
        is InstallState.Downloading -> install.size.label
        InstallState.PendingUserAction -> LIBRARY_PENDING_SUPPORTING
        is InstallState.Failed -> LIBRARY_FAILED_SUPPORTING
        else -> app.versionTag
    }
}

data class LibraryContent(
    val items: List<LibraryItem>,
) {
    val isEmpty: Boolean get() = items.isEmpty()

    val downloading: List<LibraryItem> get() = items.filter(LibraryItem::isDownloading)
}

internal const val LIBRARY_PENDING_SUPPORTING = "Waiting for confirmation"
internal const val LIBRARY_FAILED_SUPPORTING = "Install failed"
