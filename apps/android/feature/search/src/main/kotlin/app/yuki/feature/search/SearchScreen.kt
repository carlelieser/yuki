package app.yuki.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val SEARCH_SCREEN_TAG = "searchScreen"

internal const val SEARCH_TITLE = "Browse"

@Composable
fun SearchRoute(
    onListingSelected: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listings = viewModel.listings.collectAsLazyPagingItems()

    SearchScreen(
        state = state,
        listings = listings,
        callbacks = SearchCallbacks(
            onQueryChange = viewModel::onQueryChange,
            onRecentRemoved = viewModel::onRecentSearchRemoved,
            onCategorySelected = viewModel::onCategoryChange,
            onSortSelected = viewModel::onSortChange,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class SearchCallbacks(
    val onQueryChange: (String) -> Unit,
    val onRecentRemoved: (String) -> Unit,
    val onCategorySelected: (ListingCategory?) -> Unit,
    val onSortSelected: (BrowseSortOption) -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
)

@Composable
internal fun SearchScreen(
    state: UiState<SearchContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: SearchCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val content = (state as? UiState.Success)?.data

    YukiScreen(
        title = SEARCH_TITLE,
        modifier = modifier.testTag(SEARCH_SCREEN_TAG),
        trailing = content?.let { active ->
            {
                SortSelector(
                    selected = active.sort,
                    onSortSelected = callbacks.onSortSelected,
                )
            }
        },
    ) {
        BrowseScaffold(
            state = state,
            listings = listings,
            callbacks = callbacks.toBrowseCallbacks(),
            contentPadding = contentPadding,
        )
    }
}

private fun SearchCallbacks.toBrowseCallbacks(): BrowseCallbacks = BrowseCallbacks(
    onQueryChange = onQueryChange,
    onRecentRemoved = onRecentRemoved,
    onListingSelected = onListingSelected,
    categoryFilter = { content -> CategoryFilter(content.category, onCategorySelected) },
)
