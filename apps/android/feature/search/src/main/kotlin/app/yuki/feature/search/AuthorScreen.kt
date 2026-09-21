package app.yuki.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.model.ListingSummary

const val AUTHOR_SCREEN_TAG = "authorScreen"

@Composable
fun AuthorRoute(
    onListingSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onAuthorSelected: ((String) -> Unit)? = null,
    viewModel: AuthorViewModel = hiltViewModel(),
) {
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val installs by viewModel.installs.collectAsStateWithLifecycle()
    val listings = viewModel.listings.collectAsLazyPagingItems()

    AuthorScreen(
        browsed = BrowsedAuthor(author = viewModel.author, installs = installs),
        listings = listings,
        callbacks = AuthorCallbacks(
            sort = sort,
            onSortSelected = viewModel::onSortChange,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
            onBackClick = onBackClick,
            onAuthorSelected = onAuthorSelected,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

internal data class BrowsedAuthor(
    val author: String,
    val installs: ListingInstalls,
)

data class AuthorCallbacks(
    val sort: BrowseSortOption,
    val onSortSelected: (BrowseSortOption) -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val onBackClick: () -> Unit,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

@Composable
internal fun AuthorScreen(
    browsed: BrowsedAuthor,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: AuthorCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = browsed.author,
        onBackClick = callbacks.onBackClick,
        modifier = modifier.testTag(AUTHOR_SCREEN_TAG),
        trailing = {
            SortSelector(
                selected = callbacks.sort,
                onSortSelected = callbacks.onSortSelected,
            )
        },
    ) {
        YukiPullToRefresh(
            isRefreshing = listings.isRefreshing,
            onRefresh = listings::refresh,
        ) {
            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(BROWSE_LIST_TAG),
            ) {
                browseRefreshState(listings = listings)
                browseList(
                    listings = listings,
                    installs = browsed.installs,
                    onSelect = callbacks.onListingSelected,
                    onAuthorSelected = callbacks.onAuthorSelected,
                )
            }
        }
    }
}
