package app.yuki.core.model

sealed interface InstallFailure {
    data object DownloadFailed : InstallFailure

    data object Aborted : InstallFailure

    data object InsufficientStorage : InstallFailure

    data object Incompatible : InstallFailure

    data object PackageMismatch : InstallFailure

    data class Rejected(val message: String) : InstallFailure
}

sealed interface InstallState {
    data object NotInstalled : InstallState

    data class Downloading(val progress: Float) : InstallState

    data object PendingUserAction : InstallState

    data class Installed(val versionTag: String) : InstallState

    data class UpdateAvailable(val from: String, val to: String) : InstallState

    data class Failed(val reason: InstallFailure) : InstallState
}
