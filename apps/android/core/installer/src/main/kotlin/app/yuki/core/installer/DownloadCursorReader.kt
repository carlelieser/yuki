package app.yuki.core.installer

import android.app.DownloadManager
import android.database.Cursor
import app.yuki.core.model.InstallFailure

internal data class DownloadSnapshot(
    val status: Int,
    val reason: Int,
    val bytesDownloaded: Long,
    val bytesTotal: Long,
) {
    val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isFailed: Boolean get() = status == DownloadManager.STATUS_FAILED

    val fraction: Float
        get() = if (bytesTotal > 0) (bytesDownloaded.toFloat() / bytesTotal).coerceIn(0f, 1f) else 0f

    fun toFailure(downloadUrl: String): InstallException = InstallException(
        failure = if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {
            InstallFailure.InsufficientStorage
        } else {
            InstallFailure.DownloadFailed
        },
        message = "DownloadManager failed for $downloadUrl with reason $reason",
    )
}

internal class DownloadCursorReader(private val manager: DownloadManager) {
    fun read(downloadId: Long): DownloadSnapshot? {
        val query = DownloadManager.Query().setFilterById(downloadId)

        return manager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.toSnapshot() else null
        }
    }
}

private fun Cursor.toSnapshot(): DownloadSnapshot = DownloadSnapshot(
    status = longColumn(DownloadManager.COLUMN_STATUS).toInt(),
    reason = longColumn(DownloadManager.COLUMN_REASON).toInt(),
    bytesDownloaded = longColumn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
    bytesTotal = longColumn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
)

private fun Cursor.longColumn(name: String): Long {
    val index = getColumnIndex(name)
    return if (index < 0) 0L else getLong(index)
}
