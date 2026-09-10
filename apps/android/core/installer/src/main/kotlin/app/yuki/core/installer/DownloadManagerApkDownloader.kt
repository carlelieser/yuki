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
        val downloadId = manager.enqueue(source.toRequest())
        val completed = try {
            emitUntilComplete(downloadId, source)
        } finally {
            manager.remove(downloadId)
        }

        emit(DownloadProgress.Completed(resolveApk(completed, source)))
    }

    private suspend fun FlowCollector<DownloadProgress>.emitUntilComplete(
        downloadId: Long,
        source: InstallSource,
    ): DownloadSnapshot {
        while (true) {
            val snapshot = DownloadCursorReader(manager).read(downloadId)
                ?: throw InstallException(
                    InstallFailure.DownloadFailed,
                    "Download $downloadId for ${source.downloadUrl} vanished from DownloadManager",
                )

            if (snapshot.isFailed) throw snapshot.toFailure(source.downloadUrl)
            if (snapshot.isComplete) return snapshot

            emit(DownloadProgress.Running(snapshot.size))
            delay(DOWNLOAD_POLL_INTERVAL_MILLIS)
        }
    }

    private fun InstallSource.toRequest(): DownloadManager.Request =
        DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle(fileName())
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            )
            .setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName(),
            )
}

internal fun resolveApk(snapshot: DownloadSnapshot, source: InstallSource): File =
    verifyDownloadedApk(snapshot.localUri?.toLocalPath(), source)

private fun String.toLocalPath(): String? = Uri.parse(this).path

internal fun InstallSource.fileName(): String {
    val candidate = assetName ?: "$versionTag.apk"
    return candidate.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
}
