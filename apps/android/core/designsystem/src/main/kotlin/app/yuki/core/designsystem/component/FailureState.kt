package app.yuki.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.FailureReason

const val FAILURE_STATE_TAG = "failureState"
const val FAILURE_RETRY_LABEL = "Try again"

private const val DEFAULT_MISSING_MESSAGE = "We couldn't find what you were looking for."

private fun titleFor(reason: FailureReason): String = when (reason) {
    FailureReason.Offline -> "You're offline"
    FailureReason.NotFound -> "Nothing here"
    FailureReason.Unauthorized -> "Sign in to continue"
    FailureReason.EmailNotVerified -> "Verify your email"
    FailureReason.AccountExists -> "That email is taken"
    is FailureReason.Rejected -> "That didn't work"
    is FailureReason.Server -> "Yuki is having trouble"
    is FailureReason.Unexpected -> "Something went wrong"
}

private fun descriptionFor(reason: FailureReason, missingMessage: String): String = when (reason) {
    FailureReason.Offline -> "Check your connection and try again."
    FailureReason.NotFound -> missingMessage
    FailureReason.Unauthorized -> "Your session has expired. Sign in again to continue."
    FailureReason.EmailNotVerified -> "Open the link we emailed you to finish signing in."
    FailureReason.AccountExists -> "An account with that email already exists."
    is FailureReason.Rejected -> reason.explanation
    is FailureReason.Server -> "Our server is not responding right now. Try again in a moment."
    is FailureReason.Unexpected -> "This didn't load as expected. Try again."
}

@Composable
private fun FailureIcon() {
    Box(
        modifier = Modifier
            .size(YukiSize.IconLarge)
            .background(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = YukiIcons.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(YukiSize.IconSmall),
        )
    }
}

@Composable
private fun RetryButton(onRetry: (() -> Unit)?) {
    val retry = onRetry ?: return

    YukiButton(
        label = FAILURE_RETRY_LABEL,
        onClick = retry,
        modifier = Modifier.padding(top = YukiSpacing.Small),
    )
}

@Composable
fun FailureState(
    reason: FailureReason,
    modifier: Modifier = Modifier,
    missingMessage: String = DEFAULT_MISSING_MESSAGE,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.ExtraLarge, vertical = YukiSpacing.Section)
            .testTag(FAILURE_STATE_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        FailureIcon()
        Text(
            text = titleFor(reason),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = descriptionFor(reason, missingMessage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        RetryButton(onRetry = onRetry)
    }
}
