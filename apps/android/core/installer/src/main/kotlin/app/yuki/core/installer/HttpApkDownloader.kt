package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.downloadSizeOf
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

private const val BUFFER_BYTES = 64 * 1024
private const val PROGRESS_STEP_BYTES = 256L * 1024
private const val PARTIAL_SUFFIX = ".part"

internal class HttpApkDownloader(
    private val client: OkHttpClient,
    private val directory: File,
) : ApkDownloader {
    override fun download(source: InstallSource): Flow<DownloadProgress> = flow {
        val target = File(directory, source.fileName())
        val partial = File(directory, target.name + PARTIAL_SUFFIX)

        try {
            fetch(source, partial)
            moveInto(partial, target)
        } finally {
            partial.delete()
        }

        emit(DownloadProgress.Completed(verifyDownloadedApk(target.absolutePath, source)))
    }.flowOn(Dispatchers.IO)

    override fun discard(apk: File) {
        apk.delete()
    }

    private suspend fun FlowCollector<DownloadProgress>.fetch(
        source: InstallSource,
        partial: File,
    ) = coroutineScope {
        directory.mkdirs()
        val call = client.newCall(Request.Builder().url(source.downloadUrl).build())
        val watcher = launch(start = CoroutineStart.UNDISPATCHED) { cancelWhenCancelled(call) }

        try {
            call.execute().use { response -> save(response, partial, source) }
        } catch (error: IOException) {
            ensureActive()
            throw downloadFailure(error, source)
        } finally {
            watcher.cancel()
        }
    }

    private suspend fun FlowCollector<DownloadProgress>.save(
        response: Response,
        partial: File,
        source: InstallSource,
    ) {
        if (!response.isSuccessful) {
            throw InstallException(
                InstallFailure.DownloadFailed(httpStatus = response.code),
                "Download of ${source.downloadUrl} answered ${response.code}",
            )
        }

        val total = response.body.contentLength()
        emit(DownloadProgress.Running(downloadSizeOf(bytesDownloaded = 0L, bytesTotal = total)))

        response.body.byteStream().use { input ->
            partial.outputStream().use { output -> copy(input, output, total) }
        }
    }

    private suspend fun FlowCollector<DownloadProgress>.copy(
        input: InputStream,
        output: OutputStream,
        total: Long,
    ) {
        val buffer = ByteArray(BUFFER_BYTES)
        var copied = 0L
        var reported = 0L

        while (true) {
            val read = input.read(buffer)
            if (read < 0) break

            output.write(buffer, 0, read)
            copied += read
            if (copied - reported < PROGRESS_STEP_BYTES) continue

            emit(DownloadProgress.Running(downloadSizeOf(copied, total)))
            reported = copied
        }

        emit(DownloadProgress.Running(downloadSizeOf(copied, total)))
    }

    private fun moveInto(partial: File, target: File) {
        target.delete()
        if (partial.renameTo(target)) return

        throw InstallException(
            InstallFailure.DownloadUnreadable,
            "Could not move ${partial.absolutePath} to ${target.absolutePath}",
        )
    }
}

private suspend fun cancelWhenCancelled(call: Call) {
    try {
        awaitCancellation()
    } finally {
        call.cancel()
    }
}

private fun downloadFailure(error: IOException, source: InstallSource): InstallException {
    val failure = if (error.isOutOfSpace()) {
        InstallFailure.InsufficientStorage
    } else {
        InstallFailure.DownloadFailed(httpStatus = null)
    }

    return InstallException(failure, "Download of ${source.downloadUrl} failed", error)
}
