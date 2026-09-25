package app.yuki.feature.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val SEARCH_SCREEN_TAG = "searchScreen"


@Composable
fun SearchRoute(
    onListingSelected: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onAuthorSelected: ((String) -> Unit)? = null,
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
            onAuthorSelected = onAuthorSelected,
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
    val onAuthorSelected: ((String) -> Unit)? = null,
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
        title = stringResource(R.string.search_title),
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
    onAuthorSelected = onAuthorSelected,
)
