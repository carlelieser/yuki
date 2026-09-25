package app.yuki.install

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import app.yuki.R
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.message
import app.yuki.core.installer.InstallProgress
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState

internal data class InstallFailureActions(
    val onRetry: (InstallProgress) -> Unit,
    val onDismiss: (InstallProgress) -> Unit,
    val onAllowInstalls: (InstallProgress) -> Unit,
)

private enum class FailureAction(@StringRes val label: Int) {
    Retry(DesignR.string.designsystem_install_retry),
    AllowInstalls(R.string.app_install_permission_allow),
}

private fun InstallFailure?.action(): FailureAction = when (this) {
    InstallFailure.InstallPermissionMissing -> FailureAction.AllowInstalls
    else -> FailureAction.Retry
}

private fun InstallFailureActions.perform(action: FailureAction, failed: InstallProgress) =
    when (action) {
        FailureAction.Retry -> onRetry(failed)
        FailureAction.AllowInstalls -> onAllowInstalls(failed)
    }

@Composable
internal fun InstallFailureSnackbar(failed: InstallProgress?, actions: InstallFailureActions) {
    val hostState = LocalYukiSnackbarHostState.current
    val failure = (failed?.state as? InstallState.Failed)?.reason
    val message = failure?.message(failed.target.title)
    val action = failure.action()
    val actionLabel = stringResource(action.label)

    LaunchedEffect(failed) {
        val shown = failed ?: return@LaunchedEffect
        val text = message ?: return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = text,
            actionLabel = actionLabel,
            withDismissAction = true,
        )

        when (result) {
            SnackbarResult.ActionPerformed -> actions.perform(action, shown)
            SnackbarResult.Dismissed -> actions.onDismiss(shown)
        }
    }
}
