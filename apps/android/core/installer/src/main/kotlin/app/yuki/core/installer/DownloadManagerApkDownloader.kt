package app.yuki.core.installer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import app.yuki.core.model.InstallFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

internal const val DOWNLOAD_POLL_INTERVAL_MILLIS = 400L

internal class DownloadManagerApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ApkDownloader {
    private val manager: DownloadManager
        get() = context.getSystemService(DownloadManager::class.java)

    override fun download(source: InstallSource): Flow<DownloadProgress> = flow {
        val fileName = source.fileName()
        val downloadId = manager.enqueue(source.toRequest(fileName))

        try {
            emitUntilComplete(downloadId, source)
        } finally {
            manager.remove(downloadId)
        }

        emit(DownloadProgress.Completed(downloadFile(fileName)))
    }

    private suspend fun FlowCollector<DownloadProgress>.emitUntilComplete(
        downloadId: Long,
        source: InstallSource,
    ) {
        while (true) {
            val snapshot = DownloadCursorReader(manager).read(downloadId)
                ?: throw InstallException(
                    InstallFailure.DownloadFailed,
                    "Download $downloadId for ${source.downloadUrl} vanished from DownloadManager",
                )

            if (snapshot.isFailed) throw snapshot.toFailure(source.downloadUrl)
            if (snapshot.isComplete) return

            emit(DownloadProgress.Running(snapshot.fraction))
            delay(DOWNLOAD_POLL_INTERVAL_MILLIS)
        }
    }

    private fun InstallSource.toRequest(fileName: String): DownloadManager.Request =
        DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(fileName)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName,
            )

    private fun downloadFile(fileName: String): File =
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
}

private fun InstallSource.fileName(): String {
    val candidate = assetName ?: "$versionTag.apk"
    return candidate.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
}
