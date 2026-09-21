package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary

private val IDENTITY_BADGE_OVERHANG = 6.dp

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
private fun IdentityIcon(iconUrl: String?, size: Dp, isInstalled: Boolean) {
    if (!isInstalled) {
        AppIcon(iconUrl = iconUrl, size = size)
        return
    }

    Box(
        modifier = Modifier.padding(end = IDENTITY_BADGE_OVERHANG, bottom = IDENTITY_BADGE_OVERHANG),
        contentAlignment = Alignment.BottomEnd,
    ) {
        AppIcon(iconUrl = iconUrl, size = size)
        InstalledBadge(
            isInstalled = true,
            modifier = Modifier.offset(
                x = IDENTITY_BADGE_OVERHANG,
                y = IDENTITY_BADGE_OVERHANG,
            ),
        )
    }
}

@Composable
fun ListingIdentity(
    listing: ListingSummary,
    modifier: Modifier = Modifier,
    variant: ListingIdentityVariant = ListingIdentityVariant.Compact,
    isInstalled: Boolean = false,
    onAuthorClick: ((String) -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IdentityIcon(
            iconUrl = listing.iconUrl,
            size = identityIconSize(variant),
            isInstalled = isInstalled,
        )
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
                badges = listing.toBadges(onAuthorClick = onAuthorClick),
                modifier = badgeSpacing(),
            )
        }
    }
}
