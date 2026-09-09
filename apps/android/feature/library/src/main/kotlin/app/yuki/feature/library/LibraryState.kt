package app.yuki.feature.library

import app.yuki.core.designsystem.component.AppRowContent
import app.yuki.core.model.InstalledApp

data class LibraryItem(
    val app: InstalledApp,
    val canOpen: Boolean,
) {
    val githubRepoId: Long get() = app.githubRepoId

    val row: AppRowContent get() = AppRowContent(
        title = app.title,
        supporting = app.versionTag,
        iconUrl = app.iconUrl,
    )
}

data class LibraryContent(
    val items: List<LibraryItem>,
) {
    val isEmpty: Boolean get() = items.isEmpty()
}
