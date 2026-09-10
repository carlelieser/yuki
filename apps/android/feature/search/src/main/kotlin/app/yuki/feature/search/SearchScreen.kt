package app.yuki.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.SearchBar
import app.yuki.core.designsystem.component.SearchBarState
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val SEARCH_SCREEN_TAG = "searchScreen"

internal const val SEARCH_TITLE = "Browse"

@Composable
fun SearchRoute(
    onListingSelected: (String) -> Unit,
    onBackClick: () -> Unit,
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
            onBackClick = onBackClick,
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
    val onBackClick: () -> Unit,
)

@Composable
internal fun SearchScreen(
    state: UiState<SearchContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: SearchCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = SEARCH_TITLE,
        onBackClick = callbacks.onBackClick,
        modifier = modifier.testTag(SEARCH_SCREEN_TAG),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                state = SearchBarState(query = state.query()),
                onQueryChange = callbacks.onQueryChange,
                modifier = Modifier.padding(
                    horizontal = YukiSpacing.Large,
                    vertical = YukiSpacing.Small,
                ),
            )

            SearchBody(
                state = state,
                listings = listings,
                callbacks = callbacks,
                contentPadding = contentPadding,
            )
        }
    }
}

private fun UiState<SearchContent>.query(): String =
    (this as? UiState.Success)?.data?.query.orEmpty()

@Composable
private fun SearchBody(
    state: UiState<SearchContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: SearchCallbacks,
    contentPadding: PaddingValues,
) {
    val content = (state as? UiState.Success)?.data
        ?: return YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

    if (content.isSearching) {
        SearchResults(state = content, onSelect = callbacks.onListingSelected)
        return
    }

    BrowseBody(
        content = content,
        listings = listings,
        callbacks = callbacks,
        contentPadding = contentPadding,
    )
}

@Composable
private fun BrowseBody(
    content: SearchContent,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: SearchCallbacks,
    contentPadding: PaddingValues,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        FilterControls(content = content, callbacks = callbacks)

        LazyColumn(
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            recentSection(content = content, callbacks = callbacks)
            browseRefreshState(listings = listings)
            browseList(listings = listings, onSelect = callbacks.onListingSelected)
        }
    }
}

@Composable
private fun FilterControls(content: SearchContent, callbacks: SearchCallbacks) {
    Column(verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall)) {
        CategoryFilter(
            selected = content.category,
            onCategorySelected = callbacks.onCategorySelected,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YukiSpacing.Small),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SortSelector(
                selected = content.sort,
                onSortSelected = callbacks.onSortSelected,
            )
        }
    }
}

private fun LazyListScope.recentSection(
    content: SearchContent,
    callbacks: SearchCallbacks,
) {
    item {
        RecentSearches(
            entries = content.recent,
            actions = RecentSearchActions(
                onSelect = callbacks.onQueryChange,
                onRemove = callbacks.onRecentRemoved,
            ),
        )
    }
}
