package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.formatRating
import app.yuki.core.model.formatStarCount

const val LISTING_BADGE_ROW_TAG = "listingBadgeRow"

@JvmInline
value class ListingBadges(val items: List<BadgeContent>)

@Composable
private fun categoryBadge(summary: ListingSummary): BadgeContent? {
    val category = summary.category ?: return null

    return BadgeContent(
        label = category.label,
        icon = category.icon,
        description = "Category ${category.label}",
    )
}

@Composable
private fun starsBadge(summary: ListingSummary): BadgeContent {
    val noun = if (summary.stars == 1) "star" else "stars"

    return BadgeContent(
        label = formatStarCount(summary.stars),
        icon = YukiIcons.Star,
        description = "${summary.stars} $noun",
    )
}

@Composable
private fun ratingBadge(summary: ListingSummary): BadgeContent? {
    val average = summary.ratingAverage ?: return null

    return BadgeContent(
        label = formatRating(average),
        icon = YukiIcons.ThumbUp,
        description = "Rated ${formatRating(average)} out of 5",
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
            description = "By $author",
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
