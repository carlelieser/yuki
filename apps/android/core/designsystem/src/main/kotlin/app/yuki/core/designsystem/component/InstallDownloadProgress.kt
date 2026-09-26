package app.yuki.core.designsystem.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.designsystem.theme.YukiWave
import app.yuki.core.model.DownloadSize
import app.yuki.core.model.InstallState

const val INSTALL_QUEUED_TAG = "installQueued"

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
    val label = size.label()
    val lastKnown = remember { mutableStateOf(label) }
    val labelState = remember { MutableTransitionState(hasSize) }

    if (hasSize) lastKnown.value = label

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

@Composable
private fun QueuedLabel() {
    Text(
        text = stringResource(R.string.designsystem_install_queued),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.testTag(INSTALL_QUEUED_TAG),
    )
}

private enum class ProgressAccessoryKind {
    None,
    Queued,
    Download,
    Waiting,
}

private fun accessoryKindOf(state: InstallState): ProgressAccessoryKind = when (state) {
    InstallState.Queued -> ProgressAccessoryKind.Queued
    is InstallState.Downloading -> ProgressAccessoryKind.Download
    InstallState.Installing -> ProgressAccessoryKind.Waiting
    InstallState.PendingUserAction -> ProgressAccessoryKind.Waiting
    else -> ProgressAccessoryKind.None
}

@Composable
internal fun ProgressAccessory(state: InstallState, shape: InstallProgressShape) {
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
            InstallState.Queued -> QueuedLabel()
            is InstallState.Downloading -> DownloadProgress(size = settled.size, shape = shape)
            InstallState.Installing -> YukiLoadingIndicator()
            InstallState.PendingUserAction -> YukiLoadingIndicator()
            else -> Unit
        }
    }
}
