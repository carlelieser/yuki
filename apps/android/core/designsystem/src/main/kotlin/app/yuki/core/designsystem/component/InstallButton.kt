package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import app.yuki.core.model.DownloadSize
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
    InstallFailure.DownloadUnreadable -> "Download incomplete"
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
    is InstallState.Downloading -> describeDownload(state.size)
    InstallState.PendingUserAction -> "Waiting for confirmation"
    is InstallState.Installed -> "Installed, version ${state.versionTag}"
    is InstallState.UpdateAvailable -> "Update from ${state.from} to ${state.to}"
    is InstallState.Failed -> failureLabel(state.reason)
}

private fun isFilled(state: InstallState): Boolean =
    state is InstallState.NotInstalled || state is InstallState.UpdateAvailable

private fun describeDownload(size: DownloadSize): String {
    val fraction = size.fraction
        ?: return "Downloading, ${size.label}, total size unknown"

    return "Downloading, ${(fraction * 100).toInt()} percent, ${size.label}"
}

@Composable
private fun DownloadProgress(size: DownloadSize) {
    val fraction = size.fraction
    val progressModifier = Modifier.width(YukiSpacing.Section * 3)

    Column(verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall)) {
        if (fraction == null) {
            LinearProgressIndicator(modifier = progressModifier)
        } else {
            LinearProgressIndicator(progress = { fraction }, modifier = progressModifier)
        }

        Text(
            text = size.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
private fun InstallControl(
    state: InstallState,
    onAction: InstallActionHandler,
    isEnabled: Boolean,
) {
    val action = actionFor(state)

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
    isEnabled: Boolean = true,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = describe(state) },
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InstallControl(
            state = state,
            onAction = onAction,
            isEnabled = isEnabled && state !is InstallState.PendingUserAction,
        )

        if (state is InstallState.Downloading) DownloadProgress(state.size)
        if (state is InstallState.Failed) FailureNote(state.reason)
        if (state is InstallState.PendingUserAction) YukiLoadingIndicator()
    }
}
