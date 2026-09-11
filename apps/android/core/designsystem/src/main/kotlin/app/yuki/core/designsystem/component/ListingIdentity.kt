package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary

enum class ListingIdentityVariant {
    Compact,
    Detail,
}

@Composable
private fun identityTitleStyle(variant: ListingIdentityVariant): TextStyle = when (variant) {
    ListingIdentityVariant.Compact -> MaterialTheme.typography.titleSmall
    ListingIdentityVariant.Detail -> MaterialTheme.typography.headlineSmall
}

private fun identityIconSize(variant: ListingIdentityVariant): Dp = when (variant) {
    ListingIdentityVariant.Compact -> YukiSize.IconMedium
    ListingIdentityVariant.Detail -> YukiSize.IconExtraLarge
}

private fun identityTitleLines(variant: ListingIdentityVariant): Int = when (variant) {
    ListingIdentityVariant.Compact -> 1
    ListingIdentityVariant.Detail -> 2
}

private fun identityDescription(
    listing: ListingSummary,
    variant: ListingIdentityVariant,
): String? = listing.description.takeIf { variant == ListingIdentityVariant.Compact }

@Composable
fun ListingIdentity(
    listing: ListingSummary,
    modifier: Modifier = Modifier,
    variant: ListingIdentityVariant = ListingIdentityVariant.Compact,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(iconUrl = listing.iconUrl, size = identityIconSize(variant))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listing.title,
                style = identityTitleStyle(variant),
                maxLines = identityTitleLines(variant),
                overflow = TextOverflow.Ellipsis,
            )
            ProductDescription(
                description = identityDescription(listing, variant),
                maxLines = 1,
            )
            ListingBadgeRow(
                badges = listing.toBadges(),
                modifier = badgeSpacing(),
            )
        }
    }
}
