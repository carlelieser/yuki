package app.yuki.core.database

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf

internal object ProgressStatus {
    const val QUEUED = "queued"
    const val DOWNLOADING = "downloading"
    const val INSTALLING = "installing"
    const val PENDING_USER_ACTION = "pending_user_action"
    const val INSTALLED = "installed"
    const val FAILED = "failed"
}

private object FailureName {
    const val DOWNLOAD_FAILED = "download_failed"
    const val DOWNLOAD_UNREADABLE = "download_unreadable"
    const val ABORTED = "aborted"
    const val INSUFFICIENT_STORAGE = "insufficient_storage"
    const val INCOMPATIBLE = "incompatible"
    const val PACKAGE_MISMATCH = "package_mismatch"
    const val TIMED_OUT = "timed_out"
    const val REJECTED = "rejected"
}

internal fun InstallProgress.toEntity(now: Long): InstallProgressEntity =
    InstallProgressEntity(
        githubRepoId = githubRepoId,
        slug = target.slug,
        title = target.title,
        iconUrl = target.iconUrl,
        status = state.statusName(),
        versionTag = versionTag,
        bytesDownloaded = state.bytesDownloaded(),
        bytesTotal = state.bytesTotal(),
        failureReason = state.failureName(),
        failureMessage = state.rejectionMessage(),
        createdAt = now,
        updatedAt = now,
    )

internal fun InstallProgressEntity.toProgress(): InstallProgress = InstallProgress(
    target = InstallTarget(
        githubRepoId = githubRepoId,
        slug = slug,
        title = title,
        iconUrl = iconUrl,
    ),
    versionTag = versionTag,
    state = toState(),
)

private fun InstallProgressEntity.toState(): InstallState = when (status) {
    ProgressStatus.QUEUED -> InstallState.Downloading(downloadSizeOf(0L, 0L))
    ProgressStatus.DOWNLOADING ->
        InstallState.Downloading(downloadSizeOf(bytesDownloaded, bytesTotal))
    ProgressStatus.INSTALLING -> InstallState.Installing
    ProgressStatus.PENDING_USER_ACTION -> InstallState.PendingUserAction
    ProgressStatus.INSTALLED -> InstallState.Installed(versionTag)
    ProgressStatus.FAILED -> InstallState.Failed(toFailure())
    else -> throw IllegalStateException(
        "Unknown install_progress status '$status' for githubRepoId=$githubRepoId",
    )
}

private fun InstallProgressEntity.toFailure(): InstallFailure = when (failureReason) {
    FailureName.DOWNLOAD_FAILED -> InstallFailure.DownloadFailed
    FailureName.DOWNLOAD_UNREADABLE -> InstallFailure.DownloadUnreadable
    FailureName.ABORTED -> InstallFailure.Aborted
    FailureName.INSUFFICIENT_STORAGE -> InstallFailure.InsufficientStorage
    FailureName.INCOMPATIBLE -> InstallFailure.Incompatible
    FailureName.PACKAGE_MISMATCH -> InstallFailure.PackageMismatch
    FailureName.TIMED_OUT -> InstallFailure.TimedOut
    FailureName.REJECTED -> InstallFailure.Rejected(failureMessage.orEmpty())
    else -> throw IllegalStateException(
        "Unknown install_progress failure '$failureReason' for githubRepoId=$githubRepoId",
    )
}

private fun InstallState.statusName(): String = when (this) {
    is InstallState.Downloading -> ProgressStatus.DOWNLOADING
    InstallState.Installing -> ProgressStatus.INSTALLING
    InstallState.PendingUserAction -> ProgressStatus.PENDING_USER_ACTION
    is InstallState.Installed -> ProgressStatus.INSTALLED
    is InstallState.Failed -> ProgressStatus.FAILED
    InstallState.NotInstalled, is InstallState.UpdateAvailable -> ProgressStatus.QUEUED
}

private fun InstallState.bytesDownloaded(): Long =
    (this as? InstallState.Downloading)?.size?.bytesDownloaded ?: 0L

private fun InstallState.bytesTotal(): Long =
    (this as? InstallState.Downloading)?.size?.bytesTotal ?: 0L

private fun InstallState.failureName(): String? = when (this) {
    is InstallState.Failed -> reason.name()
    else -> null
}

private fun InstallState.rejectionMessage(): String? =
    ((this as? InstallState.Failed)?.reason as? InstallFailure.Rejected)?.message

private fun InstallFailure.name(): String = when (this) {
    InstallFailure.DownloadFailed -> FailureName.DOWNLOAD_FAILED
    InstallFailure.DownloadUnreadable -> FailureName.DOWNLOAD_UNREADABLE
    InstallFailure.Aborted -> FailureName.ABORTED
    InstallFailure.InsufficientStorage -> FailureName.INSUFFICIENT_STORAGE
    InstallFailure.Incompatible -> FailureName.INCOMPATIBLE
    InstallFailure.PackageMismatch -> FailureName.PACKAGE_MISMATCH
    InstallFailure.TimedOut -> FailureName.TIMED_OUT
    is InstallFailure.Rejected -> FailureName.REJECTED
}
