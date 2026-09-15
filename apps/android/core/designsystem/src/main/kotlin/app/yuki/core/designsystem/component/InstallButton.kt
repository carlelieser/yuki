package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.DownloadSize
import app.yuki.core.model.InstallState

fun interface InstallActionHandler {
    fun onAction(action: InstallAction)
}

enum class InstallAction {
    Install,
    Update,
    Cancel,
    Retry,
    Dismiss,
    Open,
    Uninstall,
}

const val INSTALL_PROGRESS_TAG = "installProgress"

enum class InstallProgressShape {
    Linear,
    Circular,
}

enum class InstallProgressPosition {
    Leading,
    Trailing,
    None,
}

private fun labelFor(state: InstallState): String = when (state) {
    InstallState.NotInstalled -> "Install"
    is InstallState.Downloading -> "Cancel"
    InstallState.Installing -> "Installing"
    InstallState.PendingUserAction -> "Waiting for confirmation"
    is InstallState.Installed -> "Open"
    is InstallState.UpdateAvailable -> "Update"
    is InstallState.Failed -> "Retry"
}

private fun actionFor(state: InstallState): InstallAction = when (state) {
    InstallState.NotInstalled -> InstallAction.Install
    is InstallState.Downloading -> InstallAction.Cancel
    InstallState.Installing -> InstallAction.Install
    InstallState.PendingUserAction -> InstallAction.Cancel
    is InstallState.Installed -> InstallAction.Open
    is InstallState.UpdateAvailable -> InstallAction.Update
    is InstallState.Failed -> InstallAction.Retry
}

private fun describe(state: InstallState): String = when (state) {
    InstallState.NotInstalled -> "Install"
    is InstallState.Downloading -> describeDownload(state.size)
    InstallState.Installing -> "Installing"
    InstallState.PendingUserAction -> "Waiting for confirmation"
    is InstallState.Installed -> "Installed, version ${state.versionTag}"
    is InstallState.UpdateAvailable -> "Update from ${state.from} to ${state.to}"
    is InstallState.Failed -> installFailureLabel(state.reason)
}

private fun isFilled(state: InstallState, canUninstall: Boolean): Boolean {
    val isPromoted = state is InstallState.Installed && canUninstall

    return state is InstallState.NotInstalled ||
        state is InstallState.UpdateAvailable ||
        isPromoted
}

private fun isBlocking(state: InstallState): Boolean = state is InstallState.Installing

private fun describeDownload(size: DownloadSize): String {
    val fraction = size.fraction ?: return "Downloading"

    return "Downloading, ${(fraction * 100).toInt()} percent, ${size.label}"
}

@Composable
private fun LinearDownloadProgress(size: DownloadSize) {
    val fraction = size.fraction
    val progressModifier = Modifier
        .width(YukiSpacing.Section * 3)
        .testTag(INSTALL_PROGRESS_TAG)

    Column(verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall)) {
        if (fraction == null) {
            LinearProgressIndicator(modifier = progressModifier)
            return@Column
        }

        LinearProgressIndicator(progress = { fraction }, modifier = progressModifier)

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
private fun CircularDownloadProgress(size: DownloadSize) {
    val fraction = size.fraction
    val progressModifier = Modifier
        .size(YukiSize.ProgressCircular)
        .testTag(INSTALL_PROGRESS_TAG)

    if (fraction == null) {
        CircularProgressIndicator(modifier = progressModifier)
    } else {
        CircularProgressIndicator(progress = { fraction }, modifier = progressModifier)
    }
}

@Composable
private fun DownloadProgress(size: DownloadSize, shape: InstallProgressShape) {
    when (shape) {
        InstallProgressShape.Linear -> LinearDownloadProgress(size = size)
        InstallProgressShape.Circular -> CircularDownloadProgress(size = size)
    }
}

@Composable
private fun ProgressAccessory(state: InstallState, shape: InstallProgressShape) {
    if (state is InstallState.Downloading) DownloadProgress(size = state.size, shape = shape)
    if (state is InstallState.Installing) YukiLoadingIndicator()
    if (state is InstallState.PendingUserAction) YukiLoadingIndicator()
}

@Composable
private fun FailureAccessory(state: InstallState, onAction: InstallActionHandler) {
    if (state !is InstallState.Failed) return

    InstallFailureBadge(reason = state.reason)
    DismissControl(onAction = onAction)
}

@Composable
private fun InstallControl(
    state: InstallState,
    onAction: InstallActionHandler,
    isEnabled: Boolean,
    presentation: InstallControlPresentation,
) {
    val action = actionFor(state)
    val isClickable = isEnabled && !isBlocking(state)

    if (presentation.isGhost) {
        YukiTextButton(
            label = labelFor(state),
            onClick = { onAction.onAction(action) },
            isEnabled = isClickable,
        )
        return
    }

    if (isFilled(state, presentation.canUninstall)) {
        YukiButton(
            label = labelFor(state),
            onClick = { onAction.onAction(action) },
            isEnabled = isClickable,
        )
        return
    }

    YukiSecondaryButton(
        label = labelFor(state),
        onClick = { onAction.onAction(action) },
        isEnabled = isClickable,
    )
}

private data class InstallControlPresentation(
    val isGhost: Boolean,
    val canUninstall: Boolean,
)

const val INSTALL_DISMISS_DESCRIPTION = "Dismiss"
const val INSTALL_UNINSTALL_LABEL = "Uninstall"

@Composable
private fun DismissControl(onAction: InstallActionHandler) {
    IconButton(onClick = { onAction.onAction(InstallAction.Dismiss) }) {
        Icon(
            imageVector = YukiIcons.Close,
            contentDescription = INSTALL_DISMISS_DESCRIPTION,
        )
    }
}

@Composable
private fun UninstallControl(onAction: InstallActionHandler, isEnabled: Boolean) {
    YukiSecondaryButton(
        label = INSTALL_UNINSTALL_LABEL,
        onClick = { onAction.onAction(InstallAction.Uninstall) },
        isEnabled = isEnabled,
    )
}

@Composable
fun InstallButton(
    state: InstallState,
    onAction: InstallActionHandler,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    isGhost: Boolean = false,
    canUninstall: Boolean = false,
    progressShape: InstallProgressShape = InstallProgressShape.Linear,
    progressPosition: InstallProgressPosition = InstallProgressPosition.Trailing,
) {
    Row(
        modifier = modifier
            .semantics { contentDescription = describe(state) },
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (progressPosition == InstallProgressPosition.Leading) {
            ProgressAccessory(state = state, shape = progressShape)
        }

        if (canUninstall && state is InstallState.Installed) {
            UninstallControl(onAction = onAction, isEnabled = isEnabled)
        }

        InstallControl(
            state = state,
            onAction = onAction,
            isEnabled = isEnabled,
            presentation = InstallControlPresentation(isGhost, canUninstall),
        )

        if (progressPosition == InstallProgressPosition.Trailing) {
            ProgressAccessory(state = state, shape = progressShape)
        }

        FailureAccessory(state = state, onAction = onAction)
    }
}
