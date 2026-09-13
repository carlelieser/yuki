package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.component.BadgeContent
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.InstallProgressPosition
import app.yuki.core.designsystem.component.InstallProgressShape
import app.yuki.core.designsystem.component.YukiBadge
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingVersion
import java.time.Instant

internal const val PRERELEASE_LABEL = "Prerelease"
internal const val NO_ASSET_LABEL = "No asset"
internal const val VERSION_ICON_DESCRIPTION = "Release"

@Composable
private fun PrereleaseBadge() {
    YukiBadge(
        content = BadgeContent(
            label = PRERELEASE_LABEL,
            icon = YukiIcons.History,
            description = PRERELEASE_LABEL,
        ),
    )
}

@Composable
private fun NoAssetBadge() {
    YukiBadge(
        content = BadgeContent(
            label = NO_ASSET_LABEL,
            icon = YukiIcons.Error,
            description = NO_ASSET_LABEL,
        ),
    )
}

@Composable
private fun TagBadge(tag: String, modifier: Modifier = Modifier) {
    YukiBadge(
        content = BadgeContent(
            label = tag,
            icon = YukiIcons.Info,
            description = tag,
        ),
        modifier = modifier,
    )
}

@Composable
private fun PublishedBadge(publishedAt: Instant?) {
    val label = formatPublished(publishedAt)

    YukiBadge(
        content = BadgeContent(
            label = label,
            icon = YukiIcons.Event,
            description = label,
        ),
    )
}

@Composable
private fun VersionMetadata(version: ListingVersion) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagBadge(
            tag = version.tag,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
        PublishedBadge(publishedAt = version.publishedAt)
        if (version.isPrerelease) PrereleaseBadge()
    }
}

@Composable
private fun VersionText(version: ListingVersion, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        Text(
            text = version.name ?: version.tag,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        VersionMetadata(version = version)
    }
}

@Composable
private fun VersionInstallControl(
    version: ListingVersion,
    installState: VersionInstallState,
    onAction: InstallActionHandler,
) {
    if (!version.hasDownloadableAsset()) {
        NoAssetBadge()
        return
    }

    InstallButton(
        state = installState.state,
        onAction = onAction,
        isEnabled = installState.isEnabled,
        isGhost = true,
        progressShape = InstallProgressShape.Circular,
        progressPosition = InstallProgressPosition.Leading,
    )
}

@Composable
internal fun ListingVersionItem(
    version: ListingVersion,
    installState: VersionInstallState,
    onAction: InstallActionHandler,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLeadingIcon(icon = YukiIcons.DeployedCode, description = VERSION_ICON_DESCRIPTION)
        VersionText(version = version, modifier = Modifier.weight(1f))
        VersionInstallControl(
            version = version,
            installState = installState,
            onAction = onAction,
        )
    }
}
