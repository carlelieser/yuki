package app.yuki.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.FeaturedCard
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.delay

const val CAROUSEL_ADVANCE_MILLIS = 4_000L
const val FEATURED_ROW_TAG = "featuredRow"

@Composable
internal fun FeaturedRow(
    listings: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listings.size) { advance(listings.size, listState) }

    LazyRow(
        state = listState,
        modifier = modifier.testTag(FEATURED_ROW_TAG),
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        items(items = listings, key = { listing -> listing.id }) { listing ->
            FeaturedCard(listing = listing, onClick = { onSelect(listing) })
        }
    }
}

private suspend fun advance(
    count: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    if (count < 2) return

    while (true) {
        delay(CAROUSEL_ADVANCE_MILLIS)
        listState.animateScrollToItem((listState.firstVisibleItemIndex + 1) % count)
    }
}
