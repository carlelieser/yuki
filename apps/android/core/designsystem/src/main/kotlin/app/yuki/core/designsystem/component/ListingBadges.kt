package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary

const val LISTING_BADGE_ROW_TAG = "listingBadgeRow"

@JvmInline
value class ListingBadges(val items: List<BadgeContent>)

@Composable
private fun categoryBadge(summary: ListingSummary): BadgeContent? {
    val category = summary.category ?: return null
    val label = category.label()

    return BadgeContent(
        label = label,
        icon = category.icon,
        description = stringResource(R.string.designsystem_badge_category, label),
    )
}

@Composable
private fun starsBadge(summary: ListingSummary): BadgeContent {
    return BadgeContent(
        label = formatStarCount(summary.stars, currentLocale()),
        icon = YukiIcons.Star,
        description = pluralStringResource(R.plurals.designsystem_badge_stars, summary.stars, summary.stars),
    )
}

@Composable
private fun ratingBadge(summary: ListingSummary): BadgeContent? {
    val average = summary.ratingAverage ?: return null
    val rating = formatRating(average, currentLocale())

    return BadgeContent(
        label = rating,
        icon = YukiIcons.ThumbUp,
        description = stringResource(R.string.designsystem_badge_rating, rating),
    )
}

@Composable
fun ListingSummary.toBadges(
    onAuthorClick: ((String) -> Unit)? = null,
): ListingBadges = ListingBadges(
    listOfNotNull(
        categoryBadge(this),
        starsBadge(this),
        ratingBadge(this),
        BadgeContent(
            label = author,
            icon = YukiIcons.Person,
            description = stringResource(R.string.designsystem_badge_author, author),
            onClick = onAuthorClick?.let { click -> { click(author) } },
        ),
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ListingBadgeRow(badges: ListingBadges, modifier: Modifier = Modifier) {
    if (badges.items.isEmpty()) return

    FlowRow(
        modifier = modifier.testTag(LISTING_BADGE_ROW_TAG),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        badges.items.forEach { item -> YukiBadge(content = item) }
    }
}
