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
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.designsystem.component.label

const val CATEGORY_SCREEN_TAG = "categoryScreen"

@Composable
fun CategoryRoute(
    onListingSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onAuthorSelected: ((String) -> Unit)? = null,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val installs by viewModel.installs.collectAsStateWithLifecycle()
    val listings = viewModel.listings.collectAsLazyPagingItems()

    CategoryScreen(
        browsed = BrowsedCategory(category = viewModel.category, installs = installs),
        listings = listings,
        callbacks = CategoryCallbacks(
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

internal data class BrowsedCategory(
    val category: ListingCategory,
    val installs: ListingInstalls,
)

data class CategoryCallbacks(
    val sort: BrowseSortOption,
    val onSortSelected: (BrowseSortOption) -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val onBackClick: () -> Unit,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

@Composable
internal fun CategoryScreen(
    browsed: BrowsedCategory,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: CategoryCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = browsed.category.label(),
        onBackClick = callbacks.onBackClick,
        modifier = modifier.testTag(CATEGORY_SCREEN_TAG),
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

