package app.yuki.core.installer

import app.yuki.core.model.downloadSizeOf
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val STATUS_RUNNING = 2
private const val STATUS_PAUSED = 4

class DownloadStallTest {
    private val clock = SteppingClock()
    private val stall = DownloadStall(clock)

    @Test
    fun `a download that keeps moving never stalls`() {
        stall.hasStalled(snapshotAt(bytes = 100L))
        clock.advance(DOWNLOAD_STALL_TIMEOUT)

        assertFalse(stall.hasStalled(snapshotAt(bytes = 200L)))
    }

    @Test
    fun `a download with no progress for the timeout has stalled`() {
        stall.hasStalled(snapshotAt(bytes = 100L))
        clock.advance(DOWNLOAD_STALL_TIMEOUT)

        assertTrue(stall.hasStalled(snapshotAt(bytes = 100L)))
    }

    @Test
    fun `a download paused for less than the timeout has not stalled`() {
        stall.hasStalled(snapshotAt(bytes = 100L, status = STATUS_PAUSED))
        clock.advance(DOWNLOAD_STALL_TIMEOUT.minusSeconds(1))

        assertFalse(stall.hasStalled(snapshotAt(bytes = 100L, status = STATUS_PAUSED)))
    }

    @Test
    fun `a change of status restarts the timeout`() {
        stall.hasStalled(snapshotAt(bytes = 100L, status = STATUS_PAUSED))
        clock.advance(DOWNLOAD_STALL_TIMEOUT.minusSeconds(1))
        stall.hasStalled(snapshotAt(bytes = 100L))
        clock.advance(Duration.ofSeconds(2))

        assertFalse(stall.hasStalled(snapshotAt(bytes = 100L)))
    }
}

private fun snapshotAt(bytes: Long, status: Int = STATUS_RUNNING) = DownloadSnapshot(
    status = status,
    reason = 0,
    size = downloadSizeOf(bytesDownloaded = bytes, bytesTotal = 1_000L),
    localUri = null,
)

private class SteppingClock : Clock() {
    private var now: Instant = Instant.EPOCH

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = ZoneOffset.UTC

    override fun withZone(zone: ZoneId): Clock = this
}
