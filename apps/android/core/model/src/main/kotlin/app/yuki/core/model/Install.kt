package app.yuki.core.model

sealed interface InstallFailure {
    data class DownloadFailed(val httpStatus: Int?) : InstallFailure

    data object DownloadUnreadable : InstallFailure

    data object NotAnApk : InstallFailure

    data object Aborted : InstallFailure

    data object InsufficientStorage : InstallFailure

    data object Incompatible : InstallFailure

    data object PackageMismatch : InstallFailure

    data object TimedOut : InstallFailure

    data class Rejected(val message: String) : InstallFailure
}

data class DownloadSize(
    val bytesDownloaded: Long,
    val bytesTotal: Long?,
) {
    init {
        require(bytesDownloaded >= 0) { "bytesDownloaded must not be negative: $bytesDownloaded" }
        require(bytesTotal == null || bytesTotal > 0) {
            "bytesTotal must be null or positive: $bytesTotal"
        }
    }

    val isTotalKnown: Boolean get() = bytesTotal != null

    val fraction: Float?
        get() = bytesTotal?.let { total ->
            (bytesDownloaded.toFloat() / total).coerceIn(0f, 1f)
        }
}

fun downloadSizeOf(bytesDownloaded: Long, bytesTotal: Long): DownloadSize = DownloadSize(
    bytesDownloaded = bytesDownloaded.coerceAtLeast(0L),
    bytesTotal = bytesTotal.takeIf { total -> total > 0L },
)

sealed interface InstallState {
    data object NotInstalled : InstallState

    data class Downloading(val size: DownloadSize) : InstallState

    data object Installing : InstallState

    data object PendingUserAction : InstallState

    data class Installed(val versionTag: String) : InstallState

    data class UpdateAvailable(val from: String, val to: String) : InstallState

    data class Failed(val reason: InstallFailure) : InstallState
}
