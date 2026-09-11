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
import app.yuki.core.designsystem.component.ScreenAction
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val EXPLORE_SCREEN_TAG = "exploreScreen"

internal const val EXPLORE_TITLE = "Explore"
internal const val EXPLORE_MISSING_MESSAGE = "There are no apps to show right now."
internal const val FEATURED_TITLE = "Featured"
private const val SETTINGS_DESCRIPTION = "Settings"
private const val FEATURED_HEADER_KEY = "featuredHeader"
private const val FEATURED_ROW_KEY = "featuredRow"

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
        ExploreBody(
            state = state,
            callbacks = callbacks,
            contentPadding = contentPadding,
        )
    }
}

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
    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .testTag(CATEGORY_SECTIONS_TAG),
    ) {
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
            )

            else -> FeaturedRowPlaceholder(modifier = Modifier.fillMaxWidth())
        }
    }
}
