package app.yuki.core.designsystem.component

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import app.yuki.core.model.ListingSummary

const val LISTING_SECTION_ARROW_DESCRIPTION = "See all"

data class ListingSectionActions(
    val onListingSelected: (ListingSummary) -> Unit,
    val onSeeAll: (() -> Unit)? = null,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

data class ListingSectionContent(
    val title: String,
    val keyPrefix: String,
    val listings: List<ListingSummary>,
    val installs: ListingInstalls = ListingInstalls(),
    val seeAllLabel: String = title,
)

fun LazyListScope.listingSection(
    content: ListingSectionContent,
    actions: ListingSectionActions,
) {
    val keyPrefix = content.keyPrefix

    item(key = keyPrefix) {
        ListingSectionHeader(
            title = content.title,
            seeAllLabel = content.seeAllLabel,
            onSeeAll = actions.onSeeAll,
        )
    }

    items(items = content.listings, key = { listing -> "$keyPrefix/${listing.id}" }) { listing ->
        ClickableProductListItem(
            content = content.installs.apply(
                listing,
                listing.toProductListItemContent(onAuthorClick = actions.onAuthorSelected),
            ),
            onClick = { actions.onListingSelected(listing) },
        )
    }
}

@Composable
private fun ListingSectionHeader(
    title: String,
    seeAllLabel: String,
    onSeeAll: (() -> Unit)?,
) {
    SectionHeader(
        title = title,
        action = onSeeAll?.let { seeAll ->
            {
                IconButton(onClick = seeAll) {
                    Icon(
                        imageVector = YukiIcons.Forward,
                        contentDescription = "$LISTING_SECTION_ARROW_DESCRIPTION $seeAllLabel",
                    )
                }
            }
        },
    )
}
