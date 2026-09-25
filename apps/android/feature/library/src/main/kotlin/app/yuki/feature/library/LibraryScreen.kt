package app.yuki.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.ClickableProductListItem
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.ListingBadges
import app.yuki.core.designsystem.component.ProductListItem
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.designsystem.component.installFailureBadge
import app.yuki.core.designsystem.component.installFailureLabel
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState

const val LIBRARY_LIST_TAG = "libraryList"
const val LIBRARY_DISMISS_TAG = "libraryDismiss"
const val LIBRARY_DISMISS_DESCRIPTION = "Dismiss"

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
            onDismiss = viewModel::onDismiss,
            onFilterSelected = viewModel::onFilterChange,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class LibraryActions(
    val onListingClick: (String) -> Unit,
    val onExploreClick: () -> Unit,
    val onDismiss: (Long) -> Unit,
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
        title = LIBRARY_TITLE,
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
                FailureState(reason = settled.reason, missingMessage = LIBRARY_MISSING_MESSAGE)
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
                rowActions = LibraryRowActions(
                    onClick = { actions.onListingClick(item.slug) },
                    onDismiss = { actions.onDismiss(item.githubRepoId) },
                ),
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun LibraryDismissButton(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier.size(YukiSize.IconSmall),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .requiredSize(YukiSize.MinimumTouchTarget)
                .clip(CircleShape)
                .clickable(
                    onClick = onDismiss,
                    role = Role.Button,
                    interactionSource = null,
                    indication = ripple(bounded = false, radius = YukiSize.MinimumTouchTarget / 2),
                )
                .testTag(LIBRARY_DISMISS_TAG),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = YukiIcons.Close,
                contentDescription = LIBRARY_DISMISS_DESCRIPTION,
            )
        }
    }
}

@Composable
private fun LibraryRow(
    item: LibraryItem,
    rowActions: LibraryRowActions,
    modifier: Modifier = Modifier,
) {
    val install = item.install

    if (install is InstallState.Failed) {
        return ProductListItem(
            content = item.listItem.copy(
                supporting = stringResource(installFailureLabel(install.reason)),
                badges = ListingBadges(listOf(installFailureBadge(install.reason))),
            ),
            modifier = modifier.clickable(onClick = rowActions.onClick),
            trailing = { LibraryDismissButton(onDismiss = rowActions.onDismiss) },
        )
    }

    ClickableProductListItem(
        content = item.listItem,
        onClick = rowActions.onClick,
        modifier = modifier,
    )
}

private data class LibraryRowActions(
    val onClick: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
private fun LibraryEmpty(onExploreClick: () -> Unit) {
    CollectionEmpty(
        content = EmptyContent(
            title = LibraryFilter.All.emptyTitle,
            description = LibraryFilter.All.emptyDescription,
            icon = YukiIcons.GridView,
            actionLabel = LIBRARY_EMPTY_ACTION,
            onAction = onExploreClick,
        ),
    )
}

@Composable
private fun LibraryNoMatches(filter: LibraryFilter, onClearFilter: () -> Unit) {
    CollectionEmpty(
        content = EmptyContent(
            title = filter.emptyTitle,
            description = filter.emptyDescription,
            icon = YukiIcons.GridView,
            actionLabel = LIBRARY_CLEAR_FILTER_ACTION,
            onAction = onClearFilter,
        ),
    )
}

internal const val LIBRARY_TITLE = "Library"
internal const val LIBRARY_EMPTY_ACTION = "Browse apps"
internal const val LIBRARY_CLEAR_FILTER_ACTION = "Clear filter"
internal const val LIBRARY_MISSING_MESSAGE = "We couldn't load your library."
