package app.yuki.feature.listing

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.component.BadgeContent
import app.yuki.core.designsystem.component.StatusCard
import app.yuki.core.designsystem.component.StatusContent
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.designsystem.component.YukiBadge
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingVersion

const val ARCHIVED_WARNING_TAG = "listingArchivedWarning"
const val NO_INSTALLABLE_VERSION_TAG = "listingNoInstallableVersion"
const val ARCHIVED_TITLE = "This project is archived"
const val NO_INSTALLABLE_VERSION_TITLE = "No installable release"

internal const val PRERELEASE_LABEL = "Prerelease"
internal const val NO_ASSET_LABEL = "No asset"
internal const val VERSION_ICON_DESCRIPTION = "Release"

private val archivedContent = StatusContent(
    title = ARCHIVED_TITLE,
    description = "The author has stopped maintaining it. It may not receive fixes or updates.",
    tone = StatusTone.Attention,
)

private val noInstallableContent = StatusContent(
    title = NO_INSTALLABLE_VERSION_TITLE,
    description = "This listing has no stable release with a downloadable asset.",
    tone = StatusTone.Attention,
)

@Composable
internal fun ArchivedWarning(modifier: Modifier = Modifier) {
    StatusCard(content = archivedContent, modifier = modifier.testTag(ARCHIVED_WARNING_TAG))
}

@Composable
internal fun NoInstallableVersionNotice(modifier: Modifier = Modifier) {
    StatusCard(
        content = noInstallableContent,
        modifier = modifier.testTag(NO_INSTALLABLE_VERSION_TAG),
    )
}

@Composable
internal fun ListingDescription(description: String, modifier: Modifier = Modifier) {
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.Large),
    )
}

@Composable
private fun LinkRowText(row: ListingLinkRow, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        Text(text = row.label, style = MaterialTheme.typography.titleSmall)
        Text(
            text = row.supporting,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ListingLinkItem(row: ListingLinkRow, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLeadingIcon(icon = row.kind.icon, description = row.label)
        LinkRowText(row = row, modifier = Modifier.weight(1f))
        ExternalLinkIcon(modifier = Modifier.size(YukiSize.IconSmall))
    }
}

@Composable
private fun RowLeadingIcon(icon: ImageVector, description: String) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(YukiSize.IconSmall),
    )
}

@Composable
private fun VersionTone(version: ListingVersion) {
    if (version.isPrerelease) {
        YukiBadge(
            content = BadgeContent(
                label = PRERELEASE_LABEL,
                icon = YukiIcons.History,
                description = PRERELEASE_LABEL,
            ),
        )
        return
    }

    if (version.downloadUrl != null) return

    YukiBadge(
        content = BadgeContent(
            label = NO_ASSET_LABEL,
            icon = YukiIcons.Error,
            description = NO_ASSET_LABEL,
        ),
    )
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
        Text(
            text = "${version.tag} · ${formatPublished(version.publishedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ListingVersionItem(version: ListingVersion, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowLeadingIcon(icon = YukiIcons.DeployedCode, description = VERSION_ICON_DESCRIPTION)
        VersionText(version = version, modifier = Modifier.weight(1f))
        VersionTone(version = version)
    }
}
