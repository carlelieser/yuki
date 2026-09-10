package app.yuki.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import app.yuki.core.designsystem.component.SearchBar
import app.yuki.core.designsystem.component.SearchBarState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

internal data class BrowseCallbacks(
    val onQueryChange: (String) -> Unit,
    val onRecentRemoved: (String) -> Unit,
    val onSortSelected: (BrowseSortOption) -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val categoryFilter: (@Composable (SearchContent) -> Unit)? = null,
)

@Composable
internal fun BrowseScaffold(
    state: UiState<SearchContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: BrowseCallbacks,
    contentPadding: PaddingValues,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BrowseSearchBar(state = state, callbacks = callbacks)

        BrowseBody(
            state = state,
            listings = listings,
            callbacks = callbacks,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun BrowseSearchBar(state: UiState<SearchContent>, callbacks: BrowseCallbacks) {
    val content = (state as? UiState.Success)?.data

    SearchBar(
        state = SearchBarState(query = content?.query.orEmpty()),
        onQueryChange = callbacks.onQueryChange,
        modifier = Modifier.padding(
            horizontal = YukiSpacing.Large,
            vertical = YukiSpacing.Small,
        ),
        trailing = content?.let { active ->
            {
                SortSelector(
                    selected = active.sort,
                    onSortSelected = callbacks.onSortSelected,
                )
            }
        },
    )
}

@Composable
private fun BrowseBody(
    state: UiState<SearchContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: BrowseCallbacks,
    contentPadding: PaddingValues,
) {
    val content = (state as? UiState.Success)?.data
        ?: return YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

    if (content.isSearching) {
        SearchResults(state = content, onSelect = callbacks.onListingSelected)
        return
    }

    BrowseListing(
        content = content,
        listings = listings,
        callbacks = callbacks,
        contentPadding = contentPadding,
    )
}

@Composable
private fun BrowseListing(
    content: SearchContent,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: BrowseCallbacks,
    contentPadding: PaddingValues,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        callbacks.categoryFilter?.invoke(content)

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

private fun LazyListScope.recentSection(
    content: SearchContent,
    callbacks: BrowseCallbacks,
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
