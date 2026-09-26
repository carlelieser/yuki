package app.yuki.core.installer

import android.app.DownloadManager
import android.content.Context
import android.os.Environment
import androidx.core.net.toUri
import app.yuki.core.model.DownloadSize
import app.yuki.core.model.InstallFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow

internal const val DOWNLOAD_POLL_INTERVAL_MILLIS = 400L

internal class DownloadManagerApkDownloader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val clock: Clock,
) : ApkDownloader {
    private val manager: DownloadManager
        get() = context.getSystemService(DownloadManager::class.java)

    private val downloadIdsByPath = ConcurrentHashMap<String, Long>()

    override fun download(source: InstallSource): Flow<DownloadProgress> = flow {
        val downloadId = manager.enqueue(source.toRequest())
        val apk = try {
            resolveApk(emitUntilComplete(downloadId, source), source)
        } catch (failure: Throwable) {
            manager.remove(downloadId)
            throw failure
        }

        downloadIdsByPath[apk.absolutePath] = downloadId
        emit(DownloadProgress.Completed(apk))
    }

    override fun discard(apk: File) {
        val downloadId = downloadIdsByPath.remove(apk.absolutePath) ?: return
        manager.remove(downloadId)
    }

    private suspend fun FlowCollector<DownloadProgress>.emitUntilComplete(
        downloadId: Long,
        source: InstallSource,
    ): DownloadSnapshot {
        var lastEmitted: DownloadSize? = null
        val stall = DownloadStall(clock)

        while (true) {
            val snapshot = DownloadCursorReader(manager).read(downloadId)
                ?: throw InstallException(
                    InstallFailure.DownloadFailed(httpStatus = null),
                    "Download $downloadId for ${source.downloadUrl} vanished from DownloadManager",
                )

            if (snapshot.isFailed) throw snapshot.toFailure(source.downloadUrl)

            if (snapshot.shouldEmit(lastEmitted)) {
                emit(DownloadProgress.Running(snapshot.size))
                lastEmitted = snapshot.size
            }

            if (snapshot.isComplete) return snapshot
            if (stall.hasStalled(snapshot)) throw stalled(downloadId, source)

            delay(DOWNLOAD_POLL_INTERVAL_MILLIS)
        }
    }

    private fun InstallSource.toRequest(): DownloadManager.Request =
        DownloadManager.Request(downloadUrl.toUri())
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

private fun stalled(downloadId: Long, source: InstallSource): InstallException = InstallException(
    InstallFailure.DownloadFailed(httpStatus = null),
    "Download $downloadId for ${source.downloadUrl} made no progress for $DOWNLOAD_STALL_TIMEOUT",
)

internal fun DownloadSnapshot.shouldEmit(lastEmitted: DownloadSize?): Boolean =
    size != lastEmitted

internal fun resolveApk(snapshot: DownloadSnapshot, source: InstallSource): File =
    verifyDownloadedApk(snapshot.localUri?.toLocalPath(), source)

private fun String.toLocalPath(): String? = toUri().path

internal fun InstallSource.fileName(): String {
    val candidate = assetName ?: "$versionTag.apk"
    return candidate.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
}
