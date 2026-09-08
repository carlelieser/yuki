package app.yuki.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.ClickableAppRow
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.UiState

const val LIBRARY_LIST_TAG = "libraryList"

@Composable
fun LibraryScreen(
    onListingClick: (String) -> Unit,
    onExploreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: LibraryViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ResumeEffect(viewModel::onResume)

    LibraryContentScreen(
        state = state,
        actions = LibraryActions(onListingClick = onListingClick, onExploreClick = onExploreClick),
        modifier = modifier,
    )
}

data class LibraryActions(
    val onListingClick: (String) -> Unit,
    val onExploreClick: () -> Unit,
)

@Composable
internal fun LibraryContentScreen(
    state: UiState<LibraryContent>,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (state) {
            is UiState.Loading -> LibraryLoading()
            is UiState.Failure -> FailureState(
                reason = state.reason,
                modifier = Modifier.padding(YukiSpacing.Large),
            )
            is UiState.Success -> LibraryList(content = state.data, actions = actions)
        }
    }
}

@Composable
private fun LibraryLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        YukiLoadingIndicator()
    }
}

@Composable
private fun LibraryList(content: LibraryContent, actions: LibraryActions) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(LIBRARY_LIST_TAG)) {
        item { SectionHeader(title = LIBRARY_TITLE) }

        if (content.isEmpty) {
            item { LibraryEmpty(onExploreClick = actions.onExploreClick) }
            return@LazyColumn
        }

        items(content.items, key = LibraryItem::githubRepoId) { item ->
            ClickableAppRow(
                content = item.row,
                onClick = { actions.onListingClick(item.app.slug) },
            )
        }
    }
}

@Composable
private fun LibraryEmpty(onExploreClick: () -> Unit) {
    CollectionEmpty(
        content = EmptyContent(
            title = LIBRARY_EMPTY_TITLE,
            description = LIBRARY_EMPTY_DESCRIPTION,
            actionLabel = LIBRARY_EMPTY_ACTION,
            onAction = onExploreClick,
        ),
        modifier = Modifier.padding(YukiSpacing.Large),
    )
}

internal const val LIBRARY_TITLE = "Library"
internal const val LIBRARY_EMPTY_TITLE = "Nothing installed yet"
internal const val LIBRARY_EMPTY_DESCRIPTION =
    "Apps you install through Yuki appear here, ready to open or update."
internal const val LIBRARY_EMPTY_ACTION = "Browse Explore"
