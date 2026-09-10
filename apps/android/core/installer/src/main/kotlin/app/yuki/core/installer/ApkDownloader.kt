package app.yuki.core.installer

import app.yuki.core.model.DownloadSize
import java.io.File
import kotlinx.coroutines.flow.Flow

sealed interface DownloadProgress {
    data class Running(val size: DownloadSize) : DownloadProgress

    data class Completed(val apk: File) : DownloadProgress
}

interface ApkDownloader {
    fun download(source: InstallSource): Flow<DownloadProgress>
}

interface ApkIdentityReader {
    fun read(apk: File): ApkIdentity
}
