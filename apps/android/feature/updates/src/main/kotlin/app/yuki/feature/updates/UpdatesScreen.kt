package app.yuki.feature.updates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

const val UPDATES_LIST_TAG = "updatesList"

@Composable
fun UpdatesScreen(
    onListingClick: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel: UpdatesViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    UpdatesContentScreen(
        state = state,
        refresh = UpdatesRefresh(
            isRefreshing = isRefreshing,
            onPullToRefresh = viewModel::onPullToRefresh,
        ),
        actions = UpdatesActions(
            onListingClick = onListingClick,
            onInstallAction = viewModel::onInstallAction,
            onRetry = viewModel::refresh,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

data class UpdatesRefresh(
    val isRefreshing: Boolean,
    val onPullToRefresh: () -> Unit,
)

@Composable
internal fun UpdatesContentScreen(
    state: UiState<UpdatesContent>,
    refresh: UpdatesRefresh,
    actions: UpdatesActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiScreen(title = stringResource(R.string.updates_title), modifier = modifier) {
        YukiPullToRefresh(
            isRefreshing = refresh.isRefreshing,
            onRefresh = refresh.onPullToRefresh,
        ) {
            UpdatesBody(state = state, actions = actions, contentPadding = contentPadding)
        }
    }
}

@Composable
private fun UpdatesBody(
    state: UiState<UpdatesContent>,
    actions: UpdatesActions,
    contentPadding: PaddingValues,
) {
    YukiAnimatedState(state = state) { settled ->
        when (settled) {
            is UiState.Loading -> YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

            is UiState.Failure -> YukiScreenCenter(contentPadding) {
                FailureState(
                    reason = settled.reason,
                    missingMessage = stringResource(R.string.updates_missing),
                    onRetry = actions.onRetry,
                )
            }

            is UiState.Success -> UpdatesList(
                content = settled.data,
                actions = actions,
                contentPadding = contentPadding,
            )
        }
    }
}

@Composable
private fun UpdatesList(
    content: UpdatesContent,
    actions: UpdatesActions,
    contentPadding: PaddingValues,
) {
    if (content.isEmpty) {
        YukiScreenCenter(contentPadding) { UpToDate() }
        return
    }

    LazyColumn(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .testTag(UPDATES_LIST_TAG),
    ) {
        updateRows(content = content, actions = actions)
        uncheckedRows(content = content, actions = actions)
    }
}

@Composable
private fun UpToDate() {
    CollectionEmpty(
        content = EmptyContent(
            title = stringResource(R.string.updates_empty_title),
            description = stringResource(R.string.updates_empty_description),
            icon = YukiIcons.Update,
        ),
    )
}
