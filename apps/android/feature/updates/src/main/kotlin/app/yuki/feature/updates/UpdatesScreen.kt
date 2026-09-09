package app.yuki.feature.updates

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
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

    UpdatesContentScreen(
        state = state,
        actions = UpdatesActions(
            onListingClick = onListingClick,
            onInstallAction = viewModel::onInstallAction,
            onRetry = viewModel::refresh,
        ),
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@Composable
internal fun UpdatesContentScreen(
    state: UiState<UpdatesContent>,
    actions: UpdatesActions,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    YukiScreen(title = UPDATES_TITLE, modifier = modifier) {
        when (state) {
            is UiState.Loading -> YukiScreenCenter(contentPadding) { YukiLoadingIndicator() }

            is UiState.Failure -> YukiScreenCenter(contentPadding) {
                FailureState(
                    reason = state.reason,
                    missingMessage = UPDATES_MISSING_MESSAGE,
                    onRetry = actions.onRetry,
                )
            }

            is UiState.Success -> UpdatesList(
                content = state.data,
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
    if (content.hasNoUpdates) {
        YukiScreenCenter(contentPadding) {
            UpToDate(hasUnchecked = content.unchecked.isNotEmpty())
        }
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
private fun UpToDate(hasUnchecked: Boolean) {
    CollectionEmpty(
        content = EmptyContent(
            title = UPDATES_EMPTY_TITLE,
            description = if (hasUnchecked) UPDATES_PARTIAL_DESCRIPTION else UPDATES_EMPTY_DESCRIPTION,
            icon = YukiIcons.Update,
        ),
    )
}

internal const val UPDATES_TITLE = "Updates"
internal const val UPDATES_EMPTY_TITLE = "Everything is up to date"
internal const val UPDATES_EMPTY_DESCRIPTION =
    "Yuki checked every app you installed and found no new versions."
internal const val UPDATES_PARTIAL_DESCRIPTION =
    "No new versions among the apps Yuki could check."
internal const val UPDATES_MISSING_MESSAGE = "We couldn't check for updates."
