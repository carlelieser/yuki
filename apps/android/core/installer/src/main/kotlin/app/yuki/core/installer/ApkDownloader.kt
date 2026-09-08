package app.yuki.core.installer

import java.io.File
import kotlinx.coroutines.flow.Flow

sealed interface DownloadProgress {
    data class Running(val fraction: Float) : DownloadProgress

    data class Completed(val apk: File) : DownloadProgress
}

interface ApkDownloader {
    fun download(source: InstallSource): Flow<DownloadProgress>
}

interface ApkIdentityReader {
    fun read(apk: File): ApkIdentity
}
