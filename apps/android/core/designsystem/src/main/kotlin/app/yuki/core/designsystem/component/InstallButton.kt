package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState

fun interface InstallActionHandler {
    fun onAction(action: InstallAction)
}

enum class InstallAction {
    Install,
    Update,
    Cancel,
    Retry,
    Open,
}

private fun failureLabel(reason: InstallFailure): String = when (reason) {
    InstallFailure.DownloadFailed -> "Download failed"
    InstallFailure.Aborted -> "Install cancelled"
    InstallFailure.InsufficientStorage -> "Not enough space"
    InstallFailure.Incompatible -> "Not compatible"
    InstallFailure.PackageMismatch -> "Different app"
    is InstallFailure.Rejected -> "Install failed"
}

private fun labelFor(state: InstallState): String = when (state) {
    InstallState.NotInstalled -> "Install"
    is InstallState.Downloading -> "Cancel"
    InstallState.PendingUserAction -> "Waiting for confirmation"
    is InstallState.Installed -> "Open"
    is InstallState.UpdateAvailable -> "Update"
    is InstallState.Failed -> "Retry"
}

private fun actionFor(state: InstallState): InstallAction = when (state) {
    InstallState.NotInstalled -> InstallAction.Install
    is InstallState.Downloading -> InstallAction.Cancel
    InstallState.PendingUserAction -> InstallAction.Cancel
    is InstallState.Installed -> InstallAction.Open
    is InstallState.UpdateAvailable -> InstallAction.Update
    is InstallState.Failed -> InstallAction.Retry
}

private fun describe(state: InstallState): String = when (state) {
    InstallState.NotInstalled -> "Install"
    is InstallState.Downloading -> "Downloading, ${(state.progress * 100).toInt()} percent"
    InstallState.PendingUserAction -> "Waiting for confirmation"
    is InstallState.Installed -> "Installed, version ${state.versionTag}"
    is InstallState.UpdateAvailable -> "Update from ${state.from} to ${state.to}"
    is InstallState.Failed -> failureLabel(state.reason)
}

private fun isFilled(state: InstallState): Boolean =
    state is InstallState.NotInstalled || state is InstallState.UpdateAvailable

@Composable
private fun DownloadProgress(progress: Float) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier.width(YukiSpacing.Section * 3),
    )
}

@Composable
private fun FailureNote(reason: InstallFailure) {
    Text(
        text = failureLabel(reason),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.error,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun InstallControl(state: InstallState, onAction: InstallActionHandler) {
    val action = actionFor(state)
    val isEnabled = state !is InstallState.PendingUserAction

    if (isFilled(state)) {
        YukiButton(
            label = labelFor(state),
            onClick = { onAction.onAction(action) },
            isEnabled = isEnabled,
        )
        return
    }

    YukiSecondaryButton(
        label = labelFor(state),
        onClick = { onAction.onAction(action) },
        isEnabled = isEnabled,
    )
}

@Composable
fun InstallButton(
    state: InstallState,
    onAction: InstallActionHandler,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = describe(state) },
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstallControl(state = state, onAction = onAction)

        if (state is InstallState.Downloading) DownloadProgress(state.progress)
        if (state is InstallState.Failed) FailureNote(state.reason)
        if (state is InstallState.PendingUserAction) YukiLoadingIndicator()
    }
}
