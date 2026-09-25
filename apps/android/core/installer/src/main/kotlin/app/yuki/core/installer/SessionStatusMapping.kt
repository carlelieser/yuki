package app.yuki.core.installer

import android.content.pm.PackageInstaller
import app.yuki.core.model.InstallFailure

internal fun SessionStatus.toOutcome(): InstallOutcome = when (code) {
    PackageInstaller.STATUS_SUCCESS -> InstallOutcome.Succeeded
    PackageInstaller.STATUS_PENDING_USER_ACTION -> InstallOutcome.AwaitingUserAction
    else -> InstallOutcome.Failed(toFailure())
}

private fun SessionStatus.toFailure(): InstallFailure = when (code) {
    PackageInstaller.STATUS_FAILURE_ABORTED -> InstallFailure.Aborted
    PackageInstaller.STATUS_FAILURE_STORAGE -> InstallFailure.InsufficientStorage
    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallFailure.Incompatible
    PackageInstaller.STATUS_FAILURE_INVALID -> InstallFailure.InvalidApk
    PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallFailure.PackageMismatch
    PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallFailure.Rejected(describe())
    else -> InstallFailure.Rejected(describe())
}

private fun SessionStatus.describe(): String = message ?: "PackageInstaller status $code"
