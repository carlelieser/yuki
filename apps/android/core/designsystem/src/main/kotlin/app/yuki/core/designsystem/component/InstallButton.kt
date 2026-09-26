package app.yuki.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.DownloadSize
import app.yuki.core.model.InstallState
import kotlin.reflect.KClass

fun interface InstallActionHandler {
    fun onAction(action: InstallAction)
}

enum class InstallAction {
    Install,
    Update,
    Cancel,
    Retry,
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

private val labels: Map<KClass<out InstallState>, Int> = mapOf(
    InstallState.NotInstalled::class to R.string.designsystem_install,
    InstallState.Downloading::class to R.string.designsystem_install_cancel,
    InstallState.Installing::class to R.string.designsystem_install_installing,
    InstallState.PendingUserAction::class to R.string.designsystem_install_pending,
    InstallState.Installed::class to R.string.designsystem_install_open,
    InstallState.UpdateAvailable::class to R.string.designsystem_install_update,
    InstallState.Failed::class to R.string.designsystem_install_retry,
)

private fun actionFor(state: InstallState): InstallAction = when (state) {
    InstallState.NotInstalled -> InstallAction.Install
    is InstallState.Downloading -> InstallAction.Cancel
    InstallState.Installing -> InstallAction.Install
    InstallState.PendingUserAction -> InstallAction.Cancel
    is InstallState.Installed -> InstallAction.Open
    is InstallState.UpdateAvailable -> InstallAction.Update
    is InstallState.Failed -> InstallAction.Retry
}

@Composable
private fun describe(state: InstallState): String = when (state) {
    InstallState.NotInstalled -> stringResource(R.string.designsystem_install)
    is InstallState.Downloading -> describeDownload(state.size)
    InstallState.Installing -> stringResource(R.string.designsystem_install_installing)
    InstallState.PendingUserAction -> stringResource(R.string.designsystem_install_pending)
    is InstallState.Installed ->
        stringResource(R.string.designsystem_install_installed_version, state.versionTag)
    is InstallState.UpdateAvailable ->
        stringResource(R.string.designsystem_install_update_range, state.from, state.to)
    is InstallState.Failed -> stringResource(R.string.designsystem_install_retry)
}

private fun isFilled(state: InstallState, canUninstall: Boolean): Boolean {
    val isPromoted = state is InstallState.Installed && canUninstall

    return state is InstallState.NotInstalled ||
        state is InstallState.UpdateAvailable ||
        isPromoted
}

private fun isBlocking(state: InstallState): Boolean = state is InstallState.Installing

@Composable
private fun describeDownload(size: DownloadSize): String {
    val fraction = size.fraction ?: return stringResource(R.string.designsystem_install_downloading)
    val percent = formatPercent(fraction, currentLocale())

    return stringResource(R.string.designsystem_install_downloading_progress, percent, size.label())
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
            label = stringResource(labels.getValue(state::class)),
            onClick = { onAction.onAction(action) },
            isEnabled = isClickable,
        )
        return
    }

    if (isFilled(state, presentation.canUninstall)) {
        YukiButton(
            label = stringResource(labels.getValue(state::class)),
            onClick = { onAction.onAction(action) },
            isEnabled = isClickable,
        )
        return
    }

    YukiSecondaryButton(
        label = stringResource(labels.getValue(state::class)),
        onClick = { onAction.onAction(action) },
        isEnabled = isClickable,
    )
}

private data class InstallControlPresentation(
    val isGhost: Boolean,
    val canUninstall: Boolean,
)

@Composable
private fun UninstallControl(onAction: InstallActionHandler, isEnabled: Boolean) {
    YukiSecondaryButton(
        label = stringResource(R.string.designsystem_install_uninstall),
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
    val description = describe(state)

    Row(
        modifier = modifier
            .semantics { contentDescription = description },
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
    }
}
