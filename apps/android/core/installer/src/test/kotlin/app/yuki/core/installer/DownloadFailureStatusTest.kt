package app.yuki.core.installer

import android.app.DownloadManager
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Test

class DownloadFailureStatusTest {
    @Test
    fun `a client error carries its http status`() {
        assertEquals(InstallFailure.DownloadFailed(httpStatus = 404), failureFor(reason = 404))
    }

    @Test
    fun `a server error carries its http status`() {
        assertEquals(InstallFailure.DownloadFailed(httpStatus = 503), failureFor(reason = 503))
    }

    @Test
    fun `a download manager error carries no http status`() {
        assertEquals(
            InstallFailure.DownloadFailed(httpStatus = null),
            failureFor(reason = DownloadManager.ERROR_HTTP_DATA_ERROR),
        )
    }

    @Test
    fun `running out of space is reported as insufficient storage`() {
        assertEquals(
            InstallFailure.InsufficientStorage,
            failureFor(reason = DownloadManager.ERROR_INSUFFICIENT_SPACE),
        )
    }
}

private fun failureFor(reason: Int): InstallFailure = DownloadSnapshot(
    status = DownloadManager.STATUS_FAILED,
    reason = reason,
    size = downloadSizeOf(bytesDownloaded = 0L, bytesTotal = 0L),
    localUri = null,
).toFailure("https://example.test/download").failure
