package app.yuki.feature.explore

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import app.yuki.core.designsystem.component.categoryLabels

const val EXPLORE_SCREEN_TAG = "exploreScreen"

internal const val EXPLORE_TITLE = "Explore"
internal const val EXPLORE_MISSING_MESSAGE = "There are no apps to show right now."
internal const val FEATURED_TITLE = "Featured"
private const val FEATURED_HEADER_KEY = "featuredHeader"
private const val FEATURED_ROW_KEY = "featuredRow"

@Composable
fun ExploreRoute(
    onListingSelected: (String) -> Unit,
    onCategorySelected: (ListingCategory) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onAuthorSelected: ((String) -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    viewModel: ExploreViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    ExploreScreen(
        state = state,
        refresh = ExploreRefresh(
            isRefreshing = isRefreshing,
            onPullToRefresh = viewModel::onPullToRefresh,
        ),
        callbacks = ExploreCallbacks(
            onRetry = viewModel::refresh,
            onListingSelected = { listing -> onListingSelected(listing.slug) },
            onCategorySelected = onCategorySelected,
            onAuthorSelected = onAuthorSelected,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
        trailing = trailing,
    )
}

data class ExploreCallbacks(
    val onRetry: () -> Unit,
    val onListingSelected: (ListingSummary) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

data class ExploreRefresh(
    val isRefreshing: Boolean,
    val onPullToRefresh: () -> Unit,
)

@Composable
internal fun ExploreScreen(
    state: UiState<ExploreContent>,
    refresh: ExploreRefresh,
    callbacks: ExploreCallbacks,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    YukiScreen(
        title = EXPLORE_TITLE,
        trailing = trailing,
        modifier = modifier.testTag(EXPLORE_SCREEN_TAG),
    ) {
        YukiPullToRefresh(
            isRefreshing = refresh.isRefreshing,
            onRefresh = refresh.onPullToRefresh,
        ) {
            ExploreBody(
                state = state,
                callbacks = callbacks,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun ExploreBody(
    state: UiState<ExploreContent>,
    callbacks: ExploreCallbacks,
    contentPadding: PaddingValues,
) {
    YukiAnimatedState(state = state) { settled ->
        when (settled) {
            is UiState.Loading -> YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

            is UiState.Failure -> YukiScreenCenter(contentPadding) {
                FailureState(
                    reason = settled.reason,
                    missingMessage = EXPLORE_MISSING_MESSAGE,
                    onRetry = callbacks.onRetry,
                )
            }

            is UiState.Success -> ExploreContentBody(
                content = settled.data,
                callbacks = callbacks,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun ExploreContentBody(
    content: ExploreContent,
    callbacks: ExploreCallbacks,
    contentPadding: PaddingValues,
) {
    val labels = categoryLabels()

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .testTag(CATEGORY_SECTIONS_TAG),
    ) {
        featuredSection(content = content, callbacks = callbacks)

        categorySections(
            content = CategorySectionsContent(
                sections = content.sections,
                installs = content.installs,
                labels = labels,
            ),
            actions = CategorySectionActions(
                onListingSelected = callbacks.onListingSelected,
                onCategorySelected = callbacks.onCategorySelected,
                onAuthorSelected = callbacks.onAuthorSelected,
            ),
        )
    }
}

private fun LazyListScope.featuredSection(
    content: ExploreContent,
    callbacks: ExploreCallbacks,
) {
    val featured = content.featured
    if (featured is UiState.Failure) return
    if (featured is UiState.Success && featured.data.isEmpty()) return

    item(key = FEATURED_HEADER_KEY) { SectionHeader(title = FEATURED_TITLE) }
    item(key = FEATURED_ROW_KEY) {
        when (featured) {
            is UiState.Success -> FeaturedRow(
                listings = featured.data,
                onSelect = callbacks.onListingSelected,
                modifier = Modifier.fillMaxWidth(),
                installs = content.installs,
                onAuthorSelected = callbacks.onAuthorSelected,
            )

            else -> FeaturedRowPlaceholder(modifier = Modifier.fillMaxWidth())
        }
    }
}
