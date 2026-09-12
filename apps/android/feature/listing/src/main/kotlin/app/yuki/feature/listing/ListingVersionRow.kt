package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.component.BadgeContent
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.YukiBadge
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingVersion

internal const val PRERELEASE_LABEL = "Prerelease"
internal const val NO_ASSET_LABEL = "No asset"
internal const val VERSION_ICON_DESCRIPTION = "Release"

private val VERSION_INSTALL_WIDTH = YukiSize.MinimumTouchTarget * 2.5f

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
private fun VersionMetadata(version: ListingVersion) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${version.tag} · ${formatPublished(version.publishedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(weight = 1f, fill = false),
        )
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
        modifier = Modifier.width(VERSION_INSTALL_WIDTH),
        isEnabled = installState.isEnabled,
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
