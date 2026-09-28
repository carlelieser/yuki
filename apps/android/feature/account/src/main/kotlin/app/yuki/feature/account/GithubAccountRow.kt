package app.yuki.feature.account

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiTextButton

const val GITHUB_ROW_TAG = "githubRow"
const val GITHUB_DISCONNECT_DIALOG_TAG = "githubDisconnectDialog"

data class GithubRowActions(
    val onConnect: () -> Unit,
    val onDisconnect: () -> Unit,
)

@Composable
internal fun GithubAccountRow(
    state: GithubConnectionState,
    position: SettingsRowPosition,
    actions: GithubRowActions,
) {
    var isConfirming by rememberSaveable { mutableStateOf(false) }
    val github = state.github

    SettingsRow(
        position = position,
        title = stringResource(R.string.account_github_title),
        supporting = state.supporting(),
        icon = YukiIcons.Github,
        isEnabled = !state.isBusy,
        onClick = when {
            github == null -> actions.onConnect
            state.canDisconnect -> { { isConfirming = true } }
            else -> actions.onDisconnect
        },
        modifier = Modifier.testTag(GITHUB_ROW_TAG),
    )

    if (isConfirming) {
        DisconnectDialog(
            onConfirm = {
                isConfirming = false
                actions.onDisconnect()
            },
            onDismiss = { isConfirming = false },
        )
    }
}

@Composable
private fun GithubConnectionState.supporting(): String {
    val username = github?.username

    return when {
        github == null -> stringResource(R.string.account_github_not_connected)
        username != null -> stringResource(R.string.account_github_username, username)
        else -> stringResource(R.string.account_github_connected)
    }
}

@Composable
private fun DisconnectDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        modifier = Modifier.testTag(GITHUB_DISCONNECT_DIALOG_TAG),
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.account_github_disconnect_title)) },
        text = { Text(text = stringResource(R.string.account_github_disconnect_body)) },
        confirmButton = {
            YukiTextButton(
                label = stringResource(R.string.account_github_disconnect_confirm),
                onClick = onConfirm,
            )
        },
        dismissButton = {
            YukiTextButton(
                label = stringResource(R.string.account_github_disconnect_cancel),
                onClick = onDismiss,
            )
        },
    )
}
