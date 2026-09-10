package app.yuki.feature.updates

import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.model.AvailableUpdate
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp

data class UpdateRow(
    val update: AvailableUpdate,
    val install: InstallState,
) {
    val githubRepoId: Long get() = update.installed.githubRepoId

    val slug: String get() = update.installed.slug

    val listItem: ProductListItemContent get() = ProductListItemContent(
        title = update.installed.title,
        supporting = "${update.installed.versionTag} $VERSION_ARROW ${update.version.tag}",
        iconUrl = update.installed.iconUrl,
    )
}

data class UncheckedApp(
    val app: InstalledApp,
    val reason: FailureReason,
) {
    val githubRepoId: Long get() = app.githubRepoId

    val slug: String get() = app.slug

    val listItem: ProductListItemContent get() = ProductListItemContent(
        title = app.title,
        supporting = app.versionTag,
        iconUrl = app.iconUrl,
    )
}

data class UpdatesContent(
    val updates: List<UpdateRow>,
    val unchecked: List<UncheckedApp>,
) {
    val isEmpty: Boolean get() = updates.isEmpty() && unchecked.isEmpty()

    val hasNoUpdates: Boolean get() = updates.isEmpty()
}

internal const val VERSION_ARROW = "→"
