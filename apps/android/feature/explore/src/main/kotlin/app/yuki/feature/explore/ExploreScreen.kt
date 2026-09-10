package app.yuki.feature.explore

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.ScreenAction
import app.yuki.core.designsystem.component.SearchBar
import app.yuki.core.designsystem.component.SearchBarState
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val EXPLORE_SCREEN_TAG = "exploreScreen"

internal const val EXPLORE_TITLE = "Explore"
internal const val EXPLORE_MISSING_MESSAGE = "There are no apps to show right now."
private const val SETTINGS_DESCRIPTION = "Settings"

@Composable
fun ExploreRoute(
    onListingSelected: (String) -> Unit,
    onCategorySelected: (ListingCategory) -> Unit,
    onSettingsClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ExploreScreen(
        state = state,
        callbacks = ExploreCallbacks(
            onQueryChange = viewModel::onQueryChange,
            onRecentRemoved = viewModel::onRecentSearchRemoved,
            onRetry = viewModel::refresh,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
            onCategorySelected = onCategorySelected,
            onSettingsClick = onSettingsClick,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class ExploreCallbacks(
    val onQueryChange: (String) -> Unit,
    val onRecentRemoved: (String) -> Unit,
    val onRetry: () -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit,
    val onSettingsClick: () -> Unit,
)

@Composable
internal fun ExploreScreen(
    state: UiState<ExploreContent>,
    callbacks: ExploreCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiScreen(
        title = EXPLORE_TITLE,
        action = ScreenAction(
            icon = YukiIcons.Settings,
            description = SETTINGS_DESCRIPTION,
            onClick = callbacks.onSettingsClick,
        ),
        modifier = modifier.testTag(EXPLORE_SCREEN_TAG),
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

            ExploreBody(
                state = state,
                callbacks = callbacks,
                contentPadding = contentPadding,
            )
        }
    }
}

private fun UiState<ExploreContent>.query(): String =
    (this as? UiState.Success)?.data?.search?.query.orEmpty()

@Composable
private fun ExploreBody(
    state: UiState<ExploreContent>,
    callbacks: ExploreCallbacks,
    contentPadding: PaddingValues,
) {
    when (state) {
        is UiState.Loading -> YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

        is UiState.Failure -> YukiScreenCenter(contentPadding) {
            FailureState(
                reason = state.reason,
                missingMessage = EXPLORE_MISSING_MESSAGE,
                onRetry = callbacks.onRetry,
            )
        }

        is UiState.Success -> ExploreContentBody(
            content = state.data,
            callbacks = callbacks,
            contentPadding = contentPadding,
        )
    }
}

@Composable
private fun ExploreContentBody(
    content: ExploreContent,
    callbacks: ExploreCallbacks,
    contentPadding: PaddingValues,
) {
    if (content.search.isSearching) {
        SearchResults(state = content.search, onSelect = callbacks.onListingSelected)
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .testTag(CATEGORY_SECTIONS_TAG),
    ) {
        recentSection(content = content, callbacks = callbacks)
        featuredSection(content = content, callbacks = callbacks)

        categorySections(
            sections = content.sections,
            actions = CategorySectionActions(
                onListingSelected = callbacks.onListingSelected,
                onCategorySelected = callbacks.onCategorySelected,
            ),
        )
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
