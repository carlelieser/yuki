package app.yuki.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.designsystem.theme.YukiWave
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
private fun LinearDownloadBar(fraction: Float?) {
    if (fraction == null) {
        LinearWavyProgressIndicator(wavelength = YukiWave.LinearWavelength)
        return
    }

    val animated = animatedProgress(fraction)

    LinearWavyProgressIndicator(
        progress = { animated },
        wavelength = YukiWave.LinearWavelength,
    )
}

@Composable
private fun DownloadSizeLabel(size: DownloadSize) {
    val hasSize = size.fraction != null
    val lastKnown = remember { mutableStateOf(size.label) }
    val labelState = remember { MutableTransitionState(hasSize) }

    if (hasSize) lastKnown.value = size.label

    labelState.targetState = hasSize

    AnimatedVisibility(
        visibleState = labelState,
        enter = fadeIn(animationSpec = YukiMotion.fade()) +
            expandVertically(animationSpec = YukiMotion.resize()),
        exit = fadeOut(animationSpec = YukiMotion.fade()) +
            shrinkVertically(animationSpec = YukiMotion.resize()),
    ) {
        Text(
            text = lastKnown.value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LinearDownloadProgress(size: DownloadSize) {
    val progressModifier = Modifier
        .width(YukiSize.ProgressLinearWidth)
        .testTag(INSTALL_PROGRESS_TAG)

    Column(
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressIndicatorCrossfade(fraction = size.fraction, modifier = progressModifier) { settled ->
            LinearDownloadBar(fraction = settled)
        }

        DownloadSizeLabel(size = size)
    }
}

@Composable
private fun CircularDownloadRing(fraction: Float?) {
    if (fraction == null) {
        CircularWavyProgressIndicator()
        return
    }

    val animated = animatedProgress(fraction)

    CircularWavyProgressIndicator(progress = { animated })
}

@Composable
private fun CircularDownloadProgress(size: DownloadSize) {
    val progressModifier = Modifier
        .size(YukiSize.ProgressCircular)
        .testTag(INSTALL_PROGRESS_TAG)

    ProgressIndicatorCrossfade(fraction = size.fraction, modifier = progressModifier) { settled ->
        CircularDownloadRing(fraction = settled)
    }
}

@Composable
private fun DownloadProgress(size: DownloadSize, shape: InstallProgressShape) {
    when (shape) {
        InstallProgressShape.Linear -> LinearDownloadProgress(size = size)
        InstallProgressShape.Circular -> CircularDownloadProgress(size = size)
    }
}

private enum class ProgressAccessoryKind {
    None,
    Download,
    Waiting,
}

private fun accessoryKindOf(state: InstallState): ProgressAccessoryKind = when (state) {
    is InstallState.Downloading -> ProgressAccessoryKind.Download
    InstallState.Installing -> ProgressAccessoryKind.Waiting
    InstallState.PendingUserAction -> ProgressAccessoryKind.Waiting
    else -> ProgressAccessoryKind.None
}

@Composable
private fun ProgressAccessory(state: InstallState, shape: InstallProgressShape) {
    AnimatedContent(
        targetState = state,
        contentKey = ::accessoryKindOf,
        transitionSpec = {
            fadeIn(animationSpec = YukiMotion.fade()) togetherWith
                fadeOut(animationSpec = YukiMotion.fade()) using
                SizeTransform(clip = false) { _, _ -> YukiMotion.resize() }
        },
        label = "installAccessory",
    ) { settled ->
        when (settled) {
            is InstallState.Downloading -> DownloadProgress(size = settled.size, shape = shape)
            InstallState.Installing -> YukiLoadingIndicator()
            InstallState.PendingUserAction -> YukiLoadingIndicator()
            else -> Unit
        }
    }
}

@Composable
private fun FailureAccessory(state: InstallState, onAction: InstallActionHandler) {
    val failure = state as? InstallState.Failed
    val lastReason = remember { mutableStateOf(failure?.reason) }

    if (failure != null) lastReason.value = failure.reason

    SideAccessory(isVisible = failure != null) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            lastReason.value?.let { reason -> InstallFailureBadge(reason = reason) }
            DismissControl(onAction = onAction)
        }
    }
}

@Composable
private fun SideAccessory(isVisible: Boolean, content: @Composable () -> Unit) {
    val transitionState = remember { MutableTransitionState(isVisible) }

    transitionState.targetState = isVisible

    AnimatedVisibility(
        visibleState = transitionState,
        enter = fadeIn(animationSpec = YukiMotion.fade()) +
            expandHorizontally(animationSpec = YukiMotion.resize()),
        exit = fadeOut(animationSpec = YukiMotion.fade()) +
            shrinkHorizontally(animationSpec = YukiMotion.resize()),
    ) {
        content()
    }
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

        SideAccessory(isVisible = canUninstall && state is InstallState.Installed) {
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
