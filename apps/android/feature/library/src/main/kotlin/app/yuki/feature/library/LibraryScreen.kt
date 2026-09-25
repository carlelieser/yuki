package app.yuki.feature.library

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.ClickableProductListItem
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.model.UiState

const val LIBRARY_LIST_TAG = "libraryList"

@Composable
fun LibraryScreen(
    onListingClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onEnter() }

    LibraryContentScreen(
        state = state,
        refresh = LibraryRefresh(
            isRefreshing = isRefreshing,
            onPullToRefresh = viewModel::onPullToRefresh,
        ),
        actions = LibraryActions(
            onListingClick = onListingClick,
            onExploreClick = onExploreClick,
            onFilterSelected = viewModel::onFilterChange,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class LibraryActions(
    val onListingClick: (String) -> Unit,
    val onExploreClick: () -> Unit,
    val onFilterSelected: (LibraryFilter) -> Unit,
)

data class LibraryRefresh(
    val isRefreshing: Boolean,
    val onPullToRefresh: () -> Unit,
)

@Composable
internal fun LibraryContentScreen(
    state: UiState<LibraryContent>,
    refresh: LibraryRefresh,
    actions: LibraryActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiScreen(
        title = stringResource(R.string.library_title),
        trailing = { LibraryFilterHeader(state = state, actions = actions) },
        modifier = modifier,
    ) {
        YukiPullToRefresh(
            isRefreshing = refresh.isRefreshing,
            onRefresh = refresh.onPullToRefresh,
        ) {
            LibraryBody(state = state, actions = actions, contentPadding = contentPadding)
        }
    }
}

@Composable
private fun LibraryFilterHeader(
    state: UiState<LibraryContent>,
    actions: LibraryActions,
) {
    val content = (state as? UiState.Success)?.data ?: return
    if (content.isEmpty) return

    LibraryFilterSelector(
        selected = content.filter,
        onFilterSelected = actions.onFilterSelected,
    )
}

@Composable
private fun LibraryBody(
    state: UiState<LibraryContent>,
    actions: LibraryActions,
    contentPadding: PaddingValues,
) {
    YukiAnimatedState(state = state) { settled ->
        when (settled) {
            is UiState.Loading -> YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

            is UiState.Failure -> YukiScreenCenter(contentPadding) {
                FailureState(reason = settled.reason, missingMessage = stringResource(R.string.library_missing))
            }

            is UiState.Success -> LibraryList(
                content = settled.data,
                actions = actions,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun LibraryList(
    content: LibraryContent,
    actions: LibraryActions,
    contentPadding: PaddingValues,
) {
    if (content.isEmpty) {
        YukiScreenCenter(contentPadding) {
            LibraryEmpty(onExploreClick = actions.onExploreClick)
        }
        return
    }

    if (content.hasNoMatches) {
        YukiScreenCenter(contentPadding) {
            LibraryNoMatches(
                filter = content.filter,
                onClearFilter = { actions.onFilterSelected(LibraryFilter.All) },
            )
        }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .testTag(LIBRARY_LIST_TAG),
    ) {
        items(content.visible, key = LibraryItem::githubRepoId) { item ->
            LibraryRow(
                item = item,
                onClick = { actions.onListingClick(item.slug) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun LibraryRow(
    item: LibraryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ClickableProductListItem(
        content = item.toListItem(),
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun LibraryEmpty(onExploreClick: () -> Unit) {
    CollectionEmpty(
        content = EmptyContent(
            title = stringResource(LibraryFilter.All.copy.emptyTitle),
            description = stringResource(LibraryFilter.All.copy.emptyDescription),
            icon = YukiIcons.GridView,
            actionLabel = stringResource(R.string.library_empty_action),
            onAction = onExploreClick,
        ),
    )
}

@Composable
private fun LibraryNoMatches(filter: LibraryFilter, onClearFilter: () -> Unit) {
    CollectionEmpty(
        content = EmptyContent(
            title = stringResource(filter.copy.emptyTitle),
            description = stringResource(filter.copy.emptyDescription),
            icon = YukiIcons.GridView,
            actionLabel = stringResource(R.string.library_clear_filter),
            onAction = onClearFilter,
        ),
    )
}
