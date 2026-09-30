package app.yuki.feature.listing

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.SeeAllButton
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.model.ListingVersion

const val VISIBLE_VERSION_COUNT = 3

@Composable
internal fun VersionSectionHeader(onSeeAll: (() -> Unit)?) {
    val title = stringResource(R.string.listing_section_versions)

    SectionHeader(
        title = title,
        action = onSeeAll?.let { seeAll ->
            { SeeAllButton(label = title, onClick = seeAll) }
        },
    )
}

internal fun LazyListScope.versionItems(
    versions: List<ListingVersion>,
    status: ListingInstallStatus,
    onVersionInstallAction: VersionInstallHandler,
) {
    items(items = versions, key = { it.tag }) { version ->
        ListingVersionItem(
            version = version,
            install = version.toInstallable()?.let { installable ->
                VersionInstallPresentation(
                    state = versionInstallState(version = version, status = status),
                    onAction = InstallActionHandler { action ->
                        onVersionInstallAction.onAction(action, installable)
                    },
                )
            },
            modifier = Modifier.animateItem(),
        )
    }
}
