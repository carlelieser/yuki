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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.FailureReason
import kotlin.reflect.KClass

const val FAILURE_STATE_TAG = "failureState"

private val titles: Map<KClass<out FailureReason>, Int> = mapOf(
    FailureReason.Offline::class to R.string.designsystem_failure_title_offline,
    FailureReason.NotFound::class to R.string.designsystem_failure_title_not_found,
    FailureReason.Unauthorized::class to R.string.designsystem_failure_title_unauthorized,
    FailureReason.EmailNotVerified::class to R.string.designsystem_failure_title_email_not_verified,
    FailureReason.AccountExists::class to R.string.designsystem_failure_title_account_exists,
    FailureReason.Rejected::class to R.string.designsystem_failure_title_rejected,
    FailureReason.Server::class to R.string.designsystem_failure_title_server,
    FailureReason.Unexpected::class to R.string.designsystem_failure_title_unexpected,
)

private val descriptions: Map<KClass<out FailureReason>, Int> = mapOf(
    FailureReason.Offline::class to R.string.designsystem_failure_description_offline,
    FailureReason.NotFound::class to R.string.designsystem_failure_description_not_found,
    FailureReason.Unauthorized::class to R.string.designsystem_failure_description_unauthorized,
    FailureReason.EmailNotVerified::class to R.string.designsystem_failure_description_email_not_verified,
    FailureReason.AccountExists::class to R.string.designsystem_failure_description_account_exists,
    FailureReason.Server::class to R.string.designsystem_failure_description_server,
    FailureReason.Unexpected::class to R.string.designsystem_failure_description_unexpected,
)

@Composable
private fun descriptionFor(reason: FailureReason, missingMessage: String?): String = when {
    reason is FailureReason.Rejected -> reason.explanation
    reason == FailureReason.NotFound && missingMessage != null -> missingMessage
    else -> stringResource(descriptions.getValue(reason::class))
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
        label = stringResource(R.string.designsystem_failure_retry),
        onClick = retry,
        modifier = Modifier.padding(top = YukiSpacing.Small),
    )
}

@Composable
fun FailureState(
    reason: FailureReason,
    modifier: Modifier = Modifier,
    missingMessage: String? = null,
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
            text = stringResource(titles.getValue(reason::class)),
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
