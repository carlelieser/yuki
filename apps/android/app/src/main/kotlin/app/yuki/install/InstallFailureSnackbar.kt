package app.yuki.install

import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.message
import app.yuki.core.installer.InstallProgress
import app.yuki.core.model.InstallState

internal data class InstallFailureActions(
    val onRetry: (InstallProgress) -> Unit,
    val onDismiss: (InstallProgress) -> Unit,
)

@Composable
internal fun InstallFailureSnackbar(failed: InstallProgress?, actions: InstallFailureActions) {
    val hostState = LocalYukiSnackbarHostState.current
    val failure = (failed?.state as? InstallState.Failed)?.reason
    val message = failure?.message(failed.target.title)
    val retryLabel = stringResource(DesignR.string.designsystem_install_retry)

    LaunchedEffect(failed) {
        val shown = failed ?: return@LaunchedEffect
        val text = message ?: return@LaunchedEffect
        val result = hostState.showSnackbar(
            message = text,
            actionLabel = retryLabel,
            withDismissAction = true,
        )

        when (result) {
            SnackbarResult.ActionPerformed -> actions.onRetry(shown)
            SnackbarResult.Dismissed -> actions.onDismiss(shown)
        }
    }
}
