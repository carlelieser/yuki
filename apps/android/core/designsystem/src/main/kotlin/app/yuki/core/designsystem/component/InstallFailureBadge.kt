package app.yuki.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.R
import app.yuki.core.model.InstallFailure
import kotlin.reflect.KClass

const val INSTALL_FAILURE_BADGE_TAG = "installFailureBadge"

private val failureLabels: Map<KClass<out InstallFailure>, Int> = mapOf(
    InstallFailure.DownloadFailed::class to R.string.designsystem_install_failure_download_failed,
    InstallFailure.DownloadUnreadable::class to R.string.designsystem_install_failure_download_unreadable,
    InstallFailure.NotAnApk::class to R.string.designsystem_install_failure_not_an_apk,
    InstallFailure.Aborted::class to R.string.designsystem_install_failure_aborted,
    InstallFailure.SessionFailed::class to R.string.designsystem_install_failure_session_failed,
    InstallFailure.InsufficientStorage::class to R.string.designsystem_install_failure_insufficient_storage,
    InstallFailure.Incompatible::class to R.string.designsystem_install_failure_incompatible,
    InstallFailure.InvalidApk::class to R.string.designsystem_install_failure_invalid_apk,
    InstallFailure.PackageMismatch::class to R.string.designsystem_install_failure_package_mismatch,
    InstallFailure.SignatureConflict::class to R.string.designsystem_install_failure_signature_conflict,
    InstallFailure.TimedOut::class to R.string.designsystem_install_failure_timed_out,
    InstallFailure.Rejected::class to R.string.designsystem_install_failure_rejected,
)

@StringRes
fun installFailureLabel(reason: InstallFailure): Int = failureLabels.getValue(reason::class)

@Composable
fun installFailureBadge(reason: InstallFailure): BadgeContent {
    val label = stringResource(installFailureLabel(reason))

    return BadgeContent(
        label = label,
        icon = YukiIcons.Error,
        description = label,
        tone = BadgeTone.Error,
    )
}

@Composable
fun InstallFailureBadge(reason: InstallFailure, modifier: Modifier = Modifier) {
    YukiBadge(
        content = installFailureBadge(reason),
        modifier = modifier.testTag(INSTALL_FAILURE_BADGE_TAG),
    )
}
