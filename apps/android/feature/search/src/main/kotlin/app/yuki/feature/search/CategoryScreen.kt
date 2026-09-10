package app.yuki.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.theme.YukiSpacing
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
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            CategorySortRow(callbacks = callbacks)

            LazyColumn(
                contentPadding = contentPadding,
                modifier = Modifier.fillMaxSize(),
            ) {
                browseRefreshState(listings = listings)
                browseList(listings = listings, onSelect = callbacks.onListingSelected)
            }
        }
    }
}

@Composable
private fun CategorySortRow(callbacks: CategoryCallbacks) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.Small),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortSelector(
            selected = callbacks.sort,
            onSortSelected = callbacks.onSortSelected,
        )
    }
}
