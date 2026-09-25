package app.yuki.core.installer

import java.time.Clock
import java.time.Duration
import java.time.Instant

internal val DOWNLOAD_STALL_TIMEOUT: Duration = Duration.ofMinutes(2)

internal class DownloadStall(private val clock: Clock) {
    private var lastProgress: DownloadProgressMark? = null
    private var changedAt: Instant = clock.instant()

    fun hasStalled(snapshot: DownloadSnapshot): Boolean {
        val mark = DownloadProgressMark(snapshot.status, snapshot.size.bytesDownloaded)
        val now = clock.instant()

        if (mark != lastProgress) {
            lastProgress = mark
            changedAt = now
        }

        return Duration.between(changedAt, now) >= DOWNLOAD_STALL_TIMEOUT
    }
}

private data class DownloadProgressMark(val status: Int, val bytesDownloaded: Long)
