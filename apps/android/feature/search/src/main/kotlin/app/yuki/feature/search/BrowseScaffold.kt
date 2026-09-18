package app.yuki.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.yuki.core.designsystem.component.SearchBar
import app.yuki.core.designsystem.component.SearchBarFocus
import app.yuki.core.designsystem.component.SearchBarState
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

private const val BROWSE_MODE_LABEL = "browseMode"

const val BROWSE_DIVIDER_TAG = "browseDivider"

const val BROWSE_LIST_TAG = "browseList"

internal data class BrowseCallbacks(
    val onQueryChange: (String) -> Unit,
    val onRecentRemoved: (String) -> Unit,
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
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }
    val content = (state as? UiState.Success)?.data
    val hasQuery = !content?.query.isNullOrEmpty()
    val isSearchMode = isFocused || hasQuery

    BackHandler(enabled = isSearchMode) {
        focusManager.clearFocus()
        callbacks.onQueryChange("")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BrowseSearchBar(
            state = state,
            callbacks = callbacks,
            focus = SearchBarFocus(onFocusChange = { focused -> isFocused = focused }),
        )

        BrowseBody(
            state = state,
            listings = listings,
            callbacks = callbacks,
            contentPadding = contentPadding,
            isSearchMode = isSearchMode,
        )
    }
}

@Composable
private fun BrowseSearchBar(
    state: UiState<SearchContent>,
    callbacks: BrowseCallbacks,
    focus: SearchBarFocus,
) {
    val content = (state as? UiState.Success)?.data

    SearchBar(
        state = SearchBarState(query = content?.query.orEmpty()),
        onQueryChange = callbacks.onQueryChange,
        modifier = Modifier.padding(
            horizontal = YukiSpacing.Large,
            vertical = YukiSpacing.Small,
        ),
        focus = focus,
    )
}

@Composable
private fun BrowseBody(
    state: UiState<SearchContent>,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: BrowseCallbacks,
    contentPadding: PaddingValues,
    isSearchMode: Boolean,
) {
    YukiAnimatedState(state = state) { settled ->
        val content = (settled as? UiState.Success)?.data
            ?: return@YukiAnimatedState YukiScreenCenter(contentPadding) {
                YukiLoadingIndicator()
            }

        AnimatedContent(
            targetState = isSearchMode,
            transitionSpec = { fadeIn(YukiMotion.fade()) togetherWith fadeOut(YukiMotion.fade()) },
            label = BROWSE_MODE_LABEL,
        ) { searching ->
            if (searching) {
                SearchMode(
                    content = content,
                    callbacks = callbacks,
                    contentPadding = contentPadding,
                )
                return@AnimatedContent
            }

            BrowseListing(
                content = content,
                listings = listings,
                callbacks = callbacks,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun SearchMode(
    content: SearchContent,
    callbacks: BrowseCallbacks,
    contentPadding: PaddingValues,
) {
    if (content.isSearching) {
        SearchResults(state = content, onSelect = callbacks.onListingSelected)
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier.fillMaxSize(),
    ) {
        recentSection(content = content, callbacks = callbacks)
    }
}

@Composable
private fun BrowseListing(
    content: SearchContent,
    listings: LazyPagingItems<ListingSummary>,
    callbacks: BrowseCallbacks,
    contentPadding: PaddingValues,
) {
    val listState = rememberLazyListState()
    val isScrolled by remember {
        derivedStateOf { listState.canScrollBackward }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        callbacks.categoryFilter?.invoke(content)

        Spacer(modifier = Modifier.height(YukiSpacing.Small))

        if (isScrolled) {
            HorizontalDivider(modifier = Modifier.testTag(BROWSE_DIVIDER_TAG))
        }

        YukiPullToRefresh(
            isRefreshing = listings.isRefreshing,
            onRefresh = listings::refresh,
        ) {
            LazyColumn(
                state = listState,
                contentPadding = contentPadding,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(BROWSE_LIST_TAG),
            ) {
                browseRefreshState(listings = listings)
                browseList(
                    listings = listings,
                    installs = content.installs,
                    onSelect = callbacks.onListingSelected,
                )
            }
        }
    }
}

internal val LazyPagingItems<ListingSummary>.isRefreshing: Boolean
    get() = loadState.refresh is LoadState.Loading && itemCount > 0

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
