package app.yuki.core.installer

import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val STATUS_RUNNING = 2
private const val STATUS_SUCCESSFUL = 8

class DownloadEmissionTest {
    @Test
    fun `the first snapshot is always emitted`() {
        val snapshot = snapshotOf(bytesDownloaded = 0L)

        assertTrue(snapshot.shouldEmit(lastEmitted = null))
    }

    @Test
    fun `a snapshot whose byte count has not moved is not re-emitted`() {
        val snapshot = snapshotOf(bytesDownloaded = 400L)

        assertFalse(snapshot.shouldEmit(lastEmitted = snapshot.size))
    }

    @Test
    fun `a snapshot whose byte count advanced is emitted`() {
        val earlier = snapshotOf(bytesDownloaded = 400L)
        val later = snapshotOf(bytesDownloaded = 700L)

        assertTrue(later.shouldEmit(lastEmitted = earlier.size))
    }

    @Test
    fun `a snapshot whose total became known is emitted even at the same byte count`() {
        val unknownTotal = snapshotOf(bytesDownloaded = 400L, bytesTotal = 0L)
        val knownTotal = snapshotOf(bytesDownloaded = 400L, bytesTotal = 1_000L)

        assertTrue(knownTotal.shouldEmit(lastEmitted = unknownTotal.size))
    }

    @Test
    fun `a completed snapshot reporting the full size is emitted after the last poll`() {
        val lastPoll = snapshotOf(bytesDownloaded = 870L)
        val completed = snapshotOf(
            bytesDownloaded = 1_000L,
            status = STATUS_SUCCESSFUL,
        )

        assertTrue(completed.shouldEmit(lastEmitted = lastPoll.size))
    }
}

private fun snapshotOf(
    bytesDownloaded: Long,
    bytesTotal: Long = 1_000L,
    status: Int = STATUS_RUNNING,
): DownloadSnapshot = DownloadSnapshot(
    status = status,
    reason = 0,
    size = downloadSizeOf(bytesDownloaded = bytesDownloaded, bytesTotal = bytesTotal),
    localUri = null,
)
