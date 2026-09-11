package app.yuki.feature.explore

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.FeaturedCard
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.delay

const val CAROUSEL_ADVANCE_MILLIS = 4_000L
const val FEATURED_ROW_TAG = "featuredRow"

private const val LOOP_MULTIPLE = 1_000

internal fun pageCountFor(size: Int): Int = if (size < 2) size else size * LOOP_MULTIPLE

internal fun startPageFor(size: Int): Int = if (size < 2) 0 else pageCountFor(size) / 2

@Composable
internal fun FeaturedRow(
    listings: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val count = listings.size
    val pagerState = rememberPagerState(
        initialPage = remember(count) { startPageFor(count) },
        pageCount = { pageCountFor(count) },
    )

    AutoAdvance(pagerState = pagerState, count = count)

    HorizontalPager(
        state = pagerState,
        modifier = modifier.testTag(FEATURED_ROW_TAG),
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        pageSize = PageSize.Fill,
        pageSpacing = YukiSpacing.Medium,
    ) { page ->
        val listing = listings[page % count]
        FeaturedCard(listing = listing, onClick = { onSelect(listing) })
    }
}

@Composable
private fun AutoAdvance(pagerState: PagerState, count: Int) {
    if (count < 2) return

    LaunchedEffect(pagerState, count) {
        while (true) {
            delay(CAROUSEL_ADVANCE_MILLIS)
            pagerState.animateScrollToPage(pagerState.currentPage + 1)
        }
    }
}
