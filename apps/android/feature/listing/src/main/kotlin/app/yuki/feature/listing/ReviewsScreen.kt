package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.UiState
import app.yuki.core.model.failureReason

const val REVIEWS_LIST_TAG = "reviewsList"

@Composable
fun ReviewsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewsViewModel = hiltViewModel(),
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val reviews = viewModel.reviews.collectAsLazyPagingItems()

    ReviewsScreen(
        content = ReviewsContent(summary = summary, reviews = reviews),
        onBackClick = onBackClick,
        onRefresh = {
            viewModel.refreshSummary()
            reviews.refresh()
        },
        modifier = modifier,
    )
}

internal data class ReviewsContent(
    val summary: UiState<RatingSummary>,
    val reviews: LazyPagingItems<Review>,
)

@Composable
internal fun ReviewsScreen(
    content: ReviewsContent,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reviews = content.reviews

    YukiDetailScreen(
        title = stringResource(R.string.listing_section_reviews),
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        YukiPullToRefresh(
            isRefreshing = reviews.loadState.refresh is LoadState.Loading && reviews.itemCount > 0,
            onRefresh = onRefresh,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(REVIEWS_LIST_TAG),
                contentPadding = PaddingValues(vertical = YukiSpacing.Large),
                verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraLarge),
            ) {
                summaryItem(content.summary)
                reviewItems(reviews)
            }
        }
    }
}

private fun LazyListScope.summaryItem(summary: UiState<RatingSummary>) {
    val loaded = (summary as? UiState.Success)?.data ?: return

    item { RatingSummaryCard(summary = loaded) }
}

private fun LazyListScope.reviewItems(reviews: LazyPagingItems<Review>) {
    items(count = reviews.itemCount) { index ->
        val review = reviews[index] ?: return@items
        ReviewCard(review = review)
    }

    item { PagingState(reviews = reviews) }
}

@Composable
private fun PagingState(reviews: LazyPagingItems<Review>) {
    val failure = reviews.loadState.refresh as? LoadState.Error
        ?: reviews.loadState.append as? LoadState.Error
    val isLoading = reviews.loadState.refresh is LoadState.Loading ||
        reviews.loadState.append is LoadState.Loading

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.Large),
        contentAlignment = Alignment.Center,
    ) {
        when {
            failure != null -> FailureState(
                reason = failure.error.failureReason(),
                missingMessage = stringResource(R.string.listing_reviews_missing),
                onRetry = reviews::retry,
            )
            isLoading -> YukiLoadingIndicator()
        }
    }
}
