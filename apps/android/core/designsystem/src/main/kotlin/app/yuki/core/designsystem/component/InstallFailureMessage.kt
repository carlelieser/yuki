package app.yuki.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.R
import app.yuki.core.model.InstallFailure
import kotlin.reflect.KClass

private val failureMessages: Map<KClass<out InstallFailure>, Int> = mapOf(
    InstallFailure.DownloadUnreadable::class to
        R.string.designsystem_install_error_download_unreadable,
    InstallFailure.NotAnApk::class to R.string.designsystem_install_error_not_an_apk,
    InstallFailure.Aborted::class to R.string.designsystem_install_error_aborted,
    InstallFailure.SessionFailed::class to R.string.designsystem_install_error_session_failed,
    InstallFailure.InsufficientStorage::class to
        R.string.designsystem_install_error_insufficient_storage,
    InstallFailure.Incompatible::class to R.string.designsystem_install_error_incompatible,
    InstallFailure.InvalidApk::class to R.string.designsystem_install_error_invalid_apk,
    InstallFailure.PackageMismatch::class to R.string.designsystem_install_error_package_mismatch,
    InstallFailure.SignatureConflict::class to
        R.string.designsystem_install_error_signature_conflict,
    InstallFailure.TimedOut::class to R.string.designsystem_install_error_timed_out,
    InstallFailure.Rejected::class to R.string.designsystem_install_error_rejected,
)

private val GONE_STATUSES = setOf(404, 410)

@StringRes
fun installFailureMessage(failure: InstallFailure): Int = when (failure) {
    is InstallFailure.DownloadFailed -> downloadFailureMessage(failure.httpStatus)
    else -> failureMessages.getValue(failure::class)
}

@StringRes
private fun downloadFailureMessage(httpStatus: Int?): Int = when (httpStatus) {
    null -> R.string.designsystem_install_error_download_offline
    in GONE_STATUSES -> R.string.designsystem_install_error_download_gone
    else -> R.string.designsystem_install_error_download_server
}

@Composable
fun InstallFailure.message(appTitle: String): String {
    val message = installFailureMessage(this)

    return when (this) {
        is InstallFailure.Rejected -> stringResource(message, appTitle, this.message)
        else -> stringResource(message, appTitle)
    }
}
