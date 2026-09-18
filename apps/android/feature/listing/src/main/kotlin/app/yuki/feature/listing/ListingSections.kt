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
import app.yuki.core.designsystem.component.StatusCard
import app.yuki.core.designsystem.component.StatusContent
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val ARCHIVED_WARNING_TAG = "listingArchivedWarning"
const val NO_INSTALLABLE_VERSION_TAG = "listingNoInstallableVersion"
const val UNINSTALL_FAILED_TAG = "listingUninstallFailed"
const val ARCHIVED_TITLE = "This project is archived"
const val NO_INSTALLABLE_VERSION_TITLE = "No installable release"
const val UNINSTALL_FAILED_TITLE = "Couldn't uninstall"

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

private val uninstallFailedContent = StatusContent(
    title = UNINSTALL_FAILED_TITLE,
    description = "The app is still installed. Try again, or remove it from Android settings.",
    tone = StatusTone.Attention,
)

@Composable
internal fun UninstallFailedNotice(modifier: Modifier = Modifier) {
    StatusCard(
        content = uninstallFailedContent,
        modifier = modifier.testTag(UNINSTALL_FAILED_TAG),
    )
}

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
internal fun ListingLinkItem(
    row: ListingLinkRow,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
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
internal fun RowLeadingIcon(icon: ImageVector, description: String) {
    Icon(
        imageVector = icon,
        contentDescription = description,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(YukiSize.IconSmall),
    )
}
