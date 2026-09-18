package app.yuki.feature.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.yuki.core.designsystem.component.ClickableProductListItem
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.ListingInstalls
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.component.toProductListItemContent
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.failureReason

private val NothingToBrowse = EmptyContent(
    title = "No apps yet",
    description = "There is nothing to browse right now. Pull down to refresh.",
)

internal fun LazyListScope.browseList(
    listings: LazyPagingItems<ListingSummary>,
    installs: ListingInstalls,
    onSelect: (ListingSummary) -> Unit,
) {
    items(count = listings.itemCount) { index ->
        val listing = listings[index] ?: return@items
        ClickableProductListItem(
            content = installs.apply(listing, listing.toProductListItemContent()),
            onClick = { onSelect(listing) },
        )
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

private enum class AppendKind {
    Idle,
    Loading,
    Error,
}

private fun appendKindOf(append: LoadState): AppendKind = when (append) {
    is LoadState.Loading -> AppendKind.Loading
    is LoadState.Error -> AppendKind.Error
    else -> AppendKind.Idle
}

@Composable
private fun AppendState(listings: LazyPagingItems<ListingSummary>) {
    AnimatedContent(
        targetState = listings.loadState.append,
        contentKey = ::appendKindOf,
        transitionSpec = {
            fadeIn(animationSpec = YukiMotion.fade()) togetherWith
                fadeOut(animationSpec = YukiMotion.fade()) using
                SizeTransform(clip = false) { _, _ -> YukiMotion.resize() }
        },
        label = APPEND_STATE_LABEL,
    ) { append ->
        when (append) {
            is LoadState.Loading -> CenteredLoading()
            is LoadState.Error -> BrowseFailure(refresh = append, onRetry = listings::retry)
            else -> Unit
        }
    }
}

private const val APPEND_STATE_LABEL = "browseAppendState"

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
