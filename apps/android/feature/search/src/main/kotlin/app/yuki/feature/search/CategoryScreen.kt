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
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary

const val CATEGORY_SCREEN_TAG = "categoryScreen"

@Composable
fun CategoryRoute(
    onListingSelected: (String) -> Unit,
    onBackClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = hiltViewModel(),
) {
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val listings = viewModel.listings.collectAsLazyPagingItems()

    CategoryScreen(
        category = viewModel.category,
        listings = listings,
        callbacks = CategoryCallbacks(
            sort = sort,
            onSortSelected = viewModel::onSortChange,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
            onBackClick = onBackClick,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class CategoryCallbacks(
    val sort: BrowseSortOption,
    val onSortSelected: (BrowseSortOption) -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val onBackClick: () -> Unit,
)

@Composable
internal fun CategoryScreen(
    category: ListingCategory,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: CategoryCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = category.label,
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
                browseList(listings = listings, onSelect = callbacks.onListingSelected)
            }
        }
    }
}

