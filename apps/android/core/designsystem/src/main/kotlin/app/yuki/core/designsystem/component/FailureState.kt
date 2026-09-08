package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.FailureReason

const val FAILURE_STATE_TAG = "failureState"
const val FAILURE_RETRY_LABEL = "Try again"

private fun titleFor(reason: FailureReason): String = when (reason) {
    FailureReason.Offline -> "No connection"
    FailureReason.NotFound -> "Not found"
    is FailureReason.Server -> "Server error"
    is FailureReason.Unexpected -> "Something went wrong"
}

private fun descriptionFor(reason: FailureReason): String = when (reason) {
    FailureReason.Offline -> "Check your network and try again."
    FailureReason.NotFound -> "This listing is no longer available."
    is FailureReason.Server -> "Yuki responded with ${reason.status}. Try again shortly."
    is FailureReason.Unexpected -> "An unexpected error stopped this from loading."
}

@Composable
fun FailureState(
    reason: FailureReason,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Card(
        shape = YukiShape.Card,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag(FAILURE_STATE_TAG),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(YukiSpacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        ) {
            Text(
                text = titleFor(reason),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = descriptionFor(reason),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )

            val retry = onRetry ?: return@Column
            Button(onClick = retry) { Text(text = FAILURE_RETRY_LABEL) }
        }
    }
}
