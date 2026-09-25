package app.yuki.core.installer

import android.app.DownloadManager
import android.database.Cursor
import app.yuki.core.model.DownloadSize
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.downloadSizeOf

internal data class DownloadSnapshot(
    val status: Int,
    val reason: Int,
    val size: DownloadSize,
    val localUri: String?,
) {
    val isComplete: Boolean get() = status == DownloadManager.STATUS_SUCCESSFUL

    val isFailed: Boolean get() = status == DownloadManager.STATUS_FAILED

    fun toFailure(downloadUrl: String): InstallException = InstallException(
        failure = failureFor(reason),
        message = "DownloadManager failed for $downloadUrl with reason $reason",
    )
}

private val HTTP_ERROR_STATUSES = 400..599

private fun failureFor(reason: Int): InstallFailure = when (reason) {
    DownloadManager.ERROR_INSUFFICIENT_SPACE -> InstallFailure.InsufficientStorage
    in HTTP_ERROR_STATUSES -> InstallFailure.DownloadFailed(httpStatus = reason)
    else -> InstallFailure.DownloadFailed(httpStatus = null)
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
    size = downloadSizeOf(
        bytesDownloaded = longColumn(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR),
        bytesTotal = longColumn(DownloadManager.COLUMN_TOTAL_SIZE_BYTES),
    ),
    localUri = stringColumn(DownloadManager.COLUMN_LOCAL_URI),
)

private fun Cursor.longColumn(name: String): Long {
    val index = getColumnIndex(name)
    return if (index < 0) 0L else getLong(index)
}

private fun Cursor.stringColumn(name: String): String? {
    val index = getColumnIndex(name)
    return if (index < 0) null else getString(index)
}
