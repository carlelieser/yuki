package app.yuki.feature.explore

import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.yuki.core.designsystem.component.FeaturedCard
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.PageIndicator
import app.yuki.core.designsystem.component.pagerPageTransition
import app.yuki.core.designsystem.theme.LocalReduceMotion
import app.yuki.core.designsystem.theme.LocalTouchExploration
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

const val FEATURED_ROW_TAG = "featuredRow"

private const val LOOP_MULTIPLE = 1_000

internal fun pageCountFor(size: Int): Int = if (size < 2) size else size * LOOP_MULTIPLE

internal fun startPageFor(size: Int): Int = if (size < 2) 0 else pageCountFor(size) / 2

@Composable
internal fun describeFeaturedListing(title: String, index: Int, total: Int): String =
    stringResource(R.string.explore_featured_description, title, index + 1, total)

private fun Modifier.trackTouch(onTouchChanged: (Boolean) -> Unit): Modifier =
    pointerInput(onTouchChanged) {
        awaitPointerEventScope {
            while (true) {
                awaitFirstDown(requireUnconsumed = false)
                onTouchChanged(true)

                var isPressed = true
                while (isPressed) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    isPressed = event.changes.any { change -> change.pressed }
                }

                onTouchChanged(false)
            }
        }
    }

@Composable
private fun AutoAdvance(
    pagerState: PagerState,
    count: Int,
    isEnabled: Boolean,
    isTouched: Boolean,
) {
    if (count < 2 || !isEnabled || isTouched) return

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(pagerState, count, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            snapshotFlow { pagerState.settledPage }.collectLatest {
                delay(YukiMotion.CarouselAdvanceMillis)

                if (!pagerState.isScrollInProgress) {
                    pagerState.animateScrollToPage(pagerState.settledPage + 1)
                }
            }
        }
    }
}

@Composable
private fun Modifier.carouselPageActions(
    pagerState: PagerState,
    count: Int,
    scope: CoroutineScope,
): Modifier {
    if (count < 2) return this

    val next = stringResource(R.string.explore_featured_next)
    val previous = stringResource(R.string.explore_featured_previous)

    return semantics {
        customActions = listOf(
            CustomAccessibilityAction(next) {
                scope.launch { pagerState.animateScrollToPage(pagerState.settledPage + 1) }
                true
            },
            CustomAccessibilityAction(previous) {
                scope.launch { pagerState.animateScrollToPage(pagerState.settledPage - 1) }
                true
            },
        )
    }
}

@Composable
internal fun FeaturedRow(
    listings: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
    modifier: Modifier = Modifier,
    installs: ListingInstalls = ListingInstalls(),
    onAuthorSelected: ((String) -> Unit)? = null,
    onSettledIndexChanged: ((Int) -> Unit)? = null,
) {
    val count = listings.size
    val isAnimated = !LocalReduceMotion.current
    val isTouchExploration = LocalTouchExploration.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = remember(count) { startPageFor(count) },
        pageCount = { pageCountFor(count) },
    )

    var isTouched by remember { mutableStateOf(false) }

    AutoAdvance(
        pagerState = pagerState,
        count = count,
        isEnabled = isAnimated && !isTouchExploration,
        isTouched = isTouched,
    )

    if (onSettledIndexChanged != null) {
        LaunchedEffect(pagerState, count, onSettledIndexChanged) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                onSettledIndexChanged(page % count)
            }
        }
    }

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .testTag(FEATURED_ROW_TAG)
                .trackTouch { touched -> isTouched = touched }
                .carouselPageActions(pagerState = pagerState, count = count, scope = scope),
            contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
            pageSize = PageSize.Fill,
            pageSpacing = YukiSpacing.Medium,
        ) { page ->
            val index = page % count
            val listing = listings[index]

            FeaturedCard(
                listing = listing,
                onClick = { onSelect(listing) },
                modifier = Modifier.pagerPageTransition(
                    offsetFromCurrent = {
                        (page - pagerState.currentPage) + pagerState.currentPageOffsetFraction
                    },
                    isAnimated = isAnimated,
                ),
                isInstalled = listing.githubRepoId in installs.installedIds,
                onAuthorClick = onAuthorSelected,
                contentDescription = describeFeaturedListing(listing.title, index, count),
            )
        }

        PageIndicator(
            pageCount = count,
            currentPage = pagerState.currentPage % count,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = YukiSpacing.Small),
            isAnimated = isAnimated,
        )
    }
}
