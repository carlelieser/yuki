package app.yuki.notifications

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.R
import app.yuki.core.designsystem.component.YukiTextButton

const val NOTIFICATION_RATIONALE_TAG = "notification_rationale"

@Composable
internal fun rememberNotificationConsent(): NotificationConsentState {
    val context = LocalContext.current
    var isAsked by rememberSaveable { mutableStateOf(false) }
    var isRationaleVisible by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { isAsked = true }

    val state = NotificationConsentState(
        isRationaleVisible = isRationaleVisible,
        onRequest = {
            val isPending = !isAsked && !NotificationConsent.isGranted(context)
            if (NotificationConsent.isRequired && isPending) isRationaleVisible = true
        },
        onDecision = { isAllowed ->
            isRationaleVisible = false
            isAsked = true
            if (isAllowed) NotificationConsent.request(launcher)
        },
    )

    return state
}

internal data class NotificationConsentState(
    val isRationaleVisible: Boolean,
    val onRequest: () -> Unit,
    val onDecision: (Boolean) -> Unit,
)

@Composable
internal fun NotificationRationaleDialog(state: NotificationConsentState) {
    if (!state.isRationaleVisible) return

    AlertDialog(
        modifier = Modifier.testTag(NOTIFICATION_RATIONALE_TAG),
        onDismissRequest = { state.onDecision(false) },
        title = { Text(text = stringResource(R.string.app_notification_rationale_title)) },
        text = { Text(text = stringResource(R.string.app_notification_rationale_message)) },
        confirmButton = {
            YukiTextButton(
                label = stringResource(R.string.app_notification_allow),
                onClick = { state.onDecision(true) },
            )
        },
        dismissButton = {
            YukiTextButton(
                label = stringResource(R.string.app_notification_not_now),
                onClick = { state.onDecision(false) },
            )
        },
    )
}
