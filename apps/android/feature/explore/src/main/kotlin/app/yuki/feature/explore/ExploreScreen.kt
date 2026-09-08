package app.yuki.feature.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.SearchBar
import app.yuki.core.designsystem.component.SearchBarState
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val EXPLORE_SCREEN_TAG = "exploreScreen"

@Composable
fun ExploreRoute(
    onListingSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listings = viewModel.listings.collectAsLazyPagingItems()

    ExploreScreen(
        state = state,
        listings = listings,
        callbacks = ExploreCallbacks(
            onQueryChange = viewModel::onQueryChange,
            onRecentRemoved = viewModel::onRecentSearchRemoved,
            onRetry = viewModel::refreshFeatured,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
        ),
        modifier = modifier,
    )
}

data class ExploreCallbacks(
    val onQueryChange: (String) -> Unit,
    val onRecentRemoved: (String) -> Unit,
    val onRetry: () -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
)

@Composable
internal fun ExploreScreen(
    state: UiState<ExploreContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: ExploreCallbacks,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier
            .fillMaxSize()
            .testTag(EXPLORE_SCREEN_TAG),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchBar(
                state = SearchBarState(query = state.query()),
                onQueryChange = callbacks.onQueryChange,
                modifier = Modifier.padding(YukiSpacing.Large),
            )

            ExploreBody(state = state, listings = listings, callbacks = callbacks)
        }
    }
}

private fun UiState<ExploreContent>.query(): String =
    (this as? UiState.Success)?.data?.search?.query.orEmpty()

@Composable
private fun ExploreBody(
    state: UiState<ExploreContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: ExploreCallbacks,
) {
    when (state) {
        is UiState.Loading -> YukiLoadingIndicator(
            modifier = Modifier.padding(YukiSpacing.ExtraLarge),
        )

        is UiState.Failure -> FailureState(
            reason = state.reason,
            modifier = Modifier.padding(YukiSpacing.Large),
            onRetry = callbacks.onRetry,
        )

        is UiState.Success -> ExploreContentBody(
            content = state.data,
            listings = listings,
            callbacks = callbacks,
        )
    }
}

@Composable
private fun ExploreContentBody(
    content: ExploreContent,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: ExploreCallbacks,
) {
    if (content.search.isSearching) {
        SearchResults(state = content.search, onSelect = callbacks.onListingSelected)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        recentSection(content = content, callbacks = callbacks)
        featuredSection(content = content, callbacks = callbacks)

        item { SectionHeader(title = "All apps") }
        browseRefreshState(listings = listings)
        browseList(listings = listings, onSelect = callbacks.onListingSelected)
    }
}

private fun LazyListScope.recentSection(
    content: ExploreContent,
    callbacks: ExploreCallbacks,
) {
    item {
        RecentSearches(
            entries = content.search.recent,
            actions = RecentSearchActions(
                onSelect = callbacks.onQueryChange,
                onRemove = callbacks.onRecentRemoved,
            ),
        )
    }
}

private fun LazyListScope.featuredSection(
    content: ExploreContent,
    callbacks: ExploreCallbacks,
) {
    val featured = content.featured as? UiState.Success ?: return
    if (featured.data.isEmpty()) return

    item { SectionHeader(title = "Featured") }
    item {
        FeaturedRow(
            listings = featured.data,
            onSelect = callbacks.onListingSelected,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
