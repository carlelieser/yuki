package app.yuki.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.ClickableProductListItem
import app.yuki.core.designsystem.component.ProductListItem
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiPullToRefresh
import app.yuki.core.designsystem.component.YukiScreen
import app.yuki.core.designsystem.component.YukiScreenCenter
import app.yuki.core.model.InstallState
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

    ResumeEffect(viewModel::onResume)

    LibraryContentScreen(
        state = state,
        refresh = LibraryRefresh(
            isRefreshing = isRefreshing,
            onPullToRefresh = viewModel::onPullToRefresh,
        ),
        actions = LibraryActions(onListingClick = onListingClick, onExploreClick = onExploreClick),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class LibraryActions(
    val onListingClick: (String) -> Unit,
    val onExploreClick: () -> Unit,
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
    YukiScreen(title = LIBRARY_TITLE, modifier = modifier) {
        YukiPullToRefresh(
            isRefreshing = refresh.isRefreshing,
            onRefresh = refresh.onPullToRefresh,
        ) {
            LibraryBody(state = state, actions = actions, contentPadding = contentPadding)
        }
    }
}

@Composable
private fun LibraryBody(
    state: UiState<LibraryContent>,
    actions: LibraryActions,
    contentPadding: PaddingValues,
) {
    when (state) {
        is UiState.Loading -> YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

        is UiState.Failure -> YukiScreenCenter(contentPadding) {
            FailureState(reason = state.reason, missingMessage = LIBRARY_MISSING_MESSAGE)
        }

        is UiState.Success -> LibraryList(
            content = state.data,
            actions = actions,
            contentPadding = contentPadding,
        )
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

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .testTag(LIBRARY_LIST_TAG),
    ) {
        items(content.items, key = LibraryItem::githubRepoId) { item ->
            LibraryRow(item = item, onClick = { actions.onListingClick(item.app.slug) })
        }
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, onClick: () -> Unit) {
    val downloading = item.install as? InstallState.Downloading
        ?: return ClickableProductListItem(content = item.listItem, onClick = onClick)

    ProductListItem(
        content = item.listItem,
        modifier = Modifier.clickable(onClick = onClick),
        trailing = { LibraryDownloadIndicator(downloading.size) },
    )
}

@Composable
private fun LibraryEmpty(onExploreClick: () -> Unit) {
    CollectionEmpty(
        content = EmptyContent(
            title = LIBRARY_EMPTY_TITLE,
            description = LIBRARY_EMPTY_DESCRIPTION,
            icon = YukiIcons.Library,
            actionLabel = LIBRARY_EMPTY_ACTION,
            onAction = onExploreClick,
        ),
    )
}

internal const val LIBRARY_TITLE = "Library"
internal const val LIBRARY_EMPTY_TITLE = "Nothing installed yet"
internal const val LIBRARY_EMPTY_DESCRIPTION =
    "Apps you install through Yuki appear here, ready to open or update."
internal const val LIBRARY_EMPTY_ACTION = "Browse apps"
internal const val LIBRARY_MISSING_MESSAGE = "We couldn't load your library."
