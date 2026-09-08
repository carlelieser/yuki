package app.yuki.feature.updates

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.UiState

const val UPDATES_LIST_TAG = "updatesList"

@Composable
fun UpdatesScreen(
    onListingClick: (String) -> Unit,
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
        modifier = modifier,
    )
}

@Composable
internal fun UpdatesContentScreen(
    state: UiState<UpdatesContent>,
    actions: UpdatesActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        when (state) {
            is UiState.Loading -> UpdatesLoading()
            is UiState.Failure -> FailureState(
                reason = state.reason,
                modifier = Modifier.padding(YukiSpacing.Large),
                onRetry = actions.onRetry,
            )
            is UiState.Success -> UpdatesList(content = state.data, actions = actions)
        }
    }
}

@Composable
private fun UpdatesLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        YukiLoadingIndicator()
    }
}

@Composable
private fun UpdatesList(content: UpdatesContent, actions: UpdatesActions) {
    LazyColumn(modifier = Modifier.fillMaxSize().testTag(UPDATES_LIST_TAG)) {
        item { SectionHeader(title = UPDATES_TITLE) }

        updateRows(content = content, actions = actions)
        uncheckedRows(content = content, actions = actions)

        if (content.hasNoUpdates) item { UpToDate(hasUnchecked = content.unchecked.isNotEmpty()) }
    }
}

@Composable
private fun UpToDate(hasUnchecked: Boolean) {
    CollectionEmpty(
        content = EmptyContent(
            title = UPDATES_EMPTY_TITLE,
            description = if (hasUnchecked) UPDATES_PARTIAL_DESCRIPTION else UPDATES_EMPTY_DESCRIPTION,
        ),
        modifier = Modifier.padding(YukiSpacing.Large),
    )
}

internal const val UPDATES_TITLE = "Updates"
internal const val UPDATES_EMPTY_TITLE = "Everything is up to date"
internal const val UPDATES_EMPTY_DESCRIPTION =
    "Yuki checked every app you installed and found no new versions."
internal const val UPDATES_PARTIAL_DESCRIPTION =
    "No new versions among the apps Yuki could check."
