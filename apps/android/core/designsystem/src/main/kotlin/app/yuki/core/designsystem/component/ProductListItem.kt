package app.yuki.core.designsystem.component

import androidx.compose.foundation.clickable
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
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary

private const val DESCRIPTION_MAX_LINES = 2

data class ProductListItemContent(
    val title: String,
    val supporting: String,
    val iconUrl: String?,
    val description: String? = null,
    val badges: ListingBadges = ListingBadges(emptyList()),
)

@Composable
fun ListingSummary.toProductListItemContent(): ProductListItemContent = ProductListItemContent(
    title = title,
    supporting = author,
    iconUrl = iconUrl,
    description = description,
    badges = toBadges(),
)

@Composable
private fun ProductListItemText(content: ProductListItemContent, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        ProductDescription(description = content.description)
        ProductListItemSupporting(content = content)
    }
}

internal fun badgeSpacing(): Modifier = Modifier.padding(top = YukiSpacing.ExtraSmall)

@Composable
internal fun ProductDescription(
    description: String?,
    maxLines: Int = DESCRIPTION_MAX_LINES,
) {
    if (description.isNullOrBlank()) return

    Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ProductListItemSupporting(content: ProductListItemContent) {
    val spacing = badgeSpacing()

    if (content.badges.items.isNotEmpty()) {
        ListingBadgeRow(badges = content.badges, modifier = spacing)
        return
    }

    Text(
        text = content.supporting,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = spacing,
    )
}

@Composable
fun ProductListItem(
    content: ProductListItemContent,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(iconUrl = content.iconUrl, size = YukiSize.IconMedium)
        ProductListItemText(content = content, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun ClickableProductListItem(
    content: ProductListItemContent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductListItem(content = content, modifier = modifier.clickable(onClick = onClick))
}
