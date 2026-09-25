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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
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
import java.time.ZoneId

@Composable
private fun PrereleaseBadge() {
    val label = stringResource(R.string.listing_version_prerelease)

    YukiBadge(
        content = BadgeContent(
            label = label,
            icon = YukiIcons.History,
            description = label,
        ),
    )
}

@Composable
private fun NoAssetBadge() {
    val label = stringResource(R.string.listing_version_no_asset)

    YukiBadge(
        content = BadgeContent(
            label = label,
            icon = YukiIcons.Error,
            description = label,
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
    val locale = LocalConfiguration.current.locales[0]
    val published = publishedAt?.let { instant ->
        formatPublished(instant, locale, ZoneId.systemDefault())
    }
    val label = published ?: stringResource(R.string.listing_version_unreleased)

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
private fun VersionInstallControl(install: VersionInstallPresentation?) {
    if (install == null) {
        NoAssetBadge()
        return
    }

    InstallButton(
        state = install.state.state,
        onAction = install.onAction,
        isEnabled = install.state.isEnabled,
        isGhost = true,
        progressShape = InstallProgressShape.Circular,
        progressPosition = InstallProgressPosition.Leading,
    )
}

@Composable
internal fun ListingVersionItem(
    version: ListingVersion,
    install: VersionInstallPresentation?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLeadingIcon(
            icon = YukiIcons.DeployedCode,
            description = stringResource(R.string.listing_version_icon),
        )
        VersionText(version = version, modifier = Modifier.weight(1f))
        VersionInstallControl(install = install)
    }
}

internal data class VersionInstallPresentation(
    val state: VersionInstallState,
    val onAction: InstallActionHandler,
)
