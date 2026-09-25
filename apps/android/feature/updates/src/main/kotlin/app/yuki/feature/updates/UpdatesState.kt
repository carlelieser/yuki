package app.yuki.feature.updates

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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

    @Composable
    fun toListItem(): ProductListItemContent = ProductListItemContent(
        title = update.installed.title,
        supporting = stringResource(
            R.string.updates_version_range,
            update.installed.versionTag,
            update.version.tag,
        ),
        iconUrl = update.installed.iconUrl,
        installState = install,
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
