package app.yuki.feature.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.yuki.core.designsystem.component.ClickableAppRow
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.toRowContent
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.failureReason

private val NothingToBrowse = EmptyContent(
    title = "No apps yet",
    description = "There is nothing to browse right now. Pull to refresh shortly.",
)

internal fun LazyListScope.browseList(
    listings: LazyPagingItems<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
) {
    items(count = listings.itemCount) { index ->
        val listing = listings[index] ?: return@items
        ClickableAppRow(content = listing.toRowContent(), onClick = { onSelect(listing) })
    }

    item { AppendState(listings = listings) }
}

internal fun LazyListScope.browseRefreshState(
    listings: LazyPagingItems<ListingSummary>,
) {
    val refresh = listings.loadState.refresh
    val isEmpty = listings.itemCount == 0

    if (refresh is LoadState.Loading && isEmpty) {
        item { CenteredLoading() }
        return
    }

    if (refresh is LoadState.Error) {
        item { BrowseFailure(refresh = refresh, onRetry = listings::retry) }
        return
    }

    val isSettled = refresh is LoadState.NotLoading
    if (isSettled && isEmpty) {
        item {
            CollectionEmpty(
                content = NothingToBrowse,
                modifier = Modifier.padding(YukiSpacing.Large),
            )
        }
    }
}

@Composable
private fun BrowseFailure(refresh: LoadState.Error, onRetry: () -> Unit) {
    FailureState(
        reason = refresh.error.failureReason(),
        modifier = Modifier.padding(YukiSpacing.Large),
        onRetry = onRetry,
    )
}

@Composable
private fun AppendState(listings: LazyPagingItems<ListingSummary>) {
    val append = listings.loadState.append

    when (append) {
        is LoadState.Loading -> CenteredLoading()
        is LoadState.Error -> BrowseFailure(refresh = append, onRetry = listings::retry)
        else -> Unit
    }
}

@Composable
private fun CenteredLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(YukiSpacing.ExtraLarge),
        contentAlignment = Alignment.Center,
    ) {
        YukiLoadingIndicator()
    }
}
