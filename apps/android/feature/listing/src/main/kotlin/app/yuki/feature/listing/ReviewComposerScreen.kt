package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.RatingInput
import app.yuki.core.designsystem.component.TextAreaContent
import app.yuki.core.designsystem.component.YukiButton
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiTextArea
import app.yuki.core.designsystem.component.YukiTextButton
import app.yuki.core.designsystem.component.failureDescription
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.FailureReason
import app.yuki.core.model.MAX_REVIEW_BODY

const val REVIEW_COMPOSER_TAG = "reviewComposer"
const val REVIEW_DELETE_DIALOG_TAG = "reviewDeleteDialog"

data class ReviewComposerCallbacks(
    val onRatingChange: (Int) -> Unit = {},
    val onBodyChange: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDeleteDismissed: () -> Unit = {},
    val onBackClick: () -> Unit = {},
    val onFailureShown: () -> Unit = {},
)

@Composable
fun ReviewComposerRoute(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ReviewComposerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isDone) {
        if (state.isDone) onDone()
    }

    ReviewComposerScreen(
        state = state,
        callbacks = ReviewComposerCallbacks(
            onRatingChange = viewModel::onRatingChange,
            onBodyChange = viewModel::onBodyChange,
            onSubmit = viewModel::submit,
            onDelete = viewModel::requestDelete,
            onDeleteConfirmed = viewModel::confirmDelete,
            onDeleteDismissed = viewModel::dismissDelete,
            onBackClick = onDone,
            onFailureShown = viewModel::onFailureShown,
        ),
        modifier = modifier,
    )
}

@Composable
internal fun ReviewComposerScreen(
    state: ReviewComposerState,
    callbacks: ReviewComposerCallbacks,
    modifier: Modifier = Modifier,
) {
    val title = if (state.isEditing) R.string.listing_composer_title_edit else R.string.listing_composer_title_write

    ReviewFailureSnackbar(failure = state.failure, onShown = callbacks.onFailureShown)

    YukiDetailScreen(
        title = stringResource(title),
        onBackClick = callbacks.onBackClick,
        modifier = modifier,
    ) {
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                YukiLoadingIndicator()
            }
            return@YukiDetailScreen
        }

        ReviewForm(state = state, callbacks = callbacks)

        if (state.isConfirmingDelete) ReviewDeleteDialog(callbacks = callbacks)
    }
}

@Composable
private fun ReviewForm(state: ReviewComposerState, callbacks: ReviewComposerCallbacks) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(YukiSpacing.Large)
            .testTag(REVIEW_COMPOSER_TAG),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RatingInput(value = state.rating, onValueChange = callbacks.onRatingChange)
        if (state.isRatingMissing) {
            Text(
                text = stringResource(R.string.listing_composer_rating_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        YukiTextArea(
            content = TextAreaContent(
                value = state.body,
                placeholder = stringResource(R.string.listing_composer_placeholder),
                maxLength = MAX_REVIEW_BODY,
            ),
            onValueChange = callbacks.onBodyChange,
        )
        ReviewFormActions(state = state, callbacks = callbacks)
    }
}

@Composable
private fun ReviewFailureSnackbar(failure: FailureReason?, onShown: () -> Unit) {
    val hostState = LocalYukiSnackbarHostState.current
    val message = failure?.let { reason -> failureDescription(reason) }

    LaunchedEffect(failure) {
        val text = message ?: return@LaunchedEffect

        hostState.showSnackbar(message = text, withDismissAction = true)
        onShown()
    }
}

@Composable
private fun ReviewFormActions(state: ReviewComposerState, callbacks: ReviewComposerCallbacks) {
    val submitLabel = if (state.isEditing) R.string.listing_composer_update else R.string.listing_composer_post

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.isEditing) {
            YukiTextButton(
                label = stringResource(R.string.listing_composer_delete),
                onClick = callbacks.onDelete,
                isEnabled = !state.isSubmitting,
            )
        }
        YukiButton(
            label = stringResource(submitLabel),
            onClick = callbacks.onSubmit,
            isEnabled = !state.isSubmitting,
        )
    }
}

@Composable
private fun ReviewDeleteDialog(callbacks: ReviewComposerCallbacks) {
    AlertDialog(
        modifier = Modifier.testTag(REVIEW_DELETE_DIALOG_TAG),
        onDismissRequest = callbacks.onDeleteDismissed,
        title = { Text(text = stringResource(R.string.listing_composer_delete_title)) },
        text = { Text(text = stringResource(R.string.listing_composer_delete_message)) },
        confirmButton = {
            YukiTextButton(
                label = stringResource(R.string.listing_composer_delete_confirm),
                onClick = callbacks.onDeleteConfirmed,
            )
        },
        dismissButton = {
            YukiTextButton(
                label = stringResource(R.string.listing_composer_delete_cancel),
                onClick = callbacks.onDeleteDismissed,
            )
        },
    )
}
