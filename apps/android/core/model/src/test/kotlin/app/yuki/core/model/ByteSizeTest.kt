package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ByteSizeTest {
    @Test
    fun `bytes below a kilobyte keep the byte unit`() {
        assertEquals("0 B", formatByteSize(0L))
        assertEquals("999 B", formatByteSize(999L))
    }

    @Test
    fun `the kilobyte boundary switches unit`() {
        assertEquals("1 KB", formatByteSize(1_000L))
        assertEquals("1.5 KB", formatByteSize(1_500L))
    }

    @Test
    fun `the megabyte boundary switches unit`() {
        assertEquals("999.9 KB", formatByteSize(999_900L))
        assertEquals("1 MB", formatByteSize(1_000_000L))
        assertEquals("12.1 MB", formatByteSize(12_100_000L))
    }

    @Test
    fun `the gigabyte boundary switches unit`() {
        assertEquals("999.9 MB", formatByteSize(999_900_000L))
        assertEquals("1 GB", formatByteSize(1_000_000_000L))
        assertEquals("2.5 GB", formatByteSize(2_500_000_000L))
    }

    @Test
    fun `the terabyte unit is the largest and does not overflow`() {
        assertEquals("1 TB", formatByteSize(1_000_000_000_000L))
        assertEquals("1500 TB", formatByteSize(1_500_000_000_000_000L))
    }

    @Test
    fun `units are capitalized the same way star counts are`() {
        assertEquals("4.1 MB", formatByteSize(4_100_000L))
        assertEquals("4.1 KB", formatByteSize(4_100L))
    }

    @Test
    fun `a known total reads as downloaded over total`() {
        assertEquals("4.1 MB / 12.1 MB", formatByteProgress(4_100_000L, 12_100_000L))
    }

    @Test
    fun `an unknown total is reported honestly rather than as zero`() {
        assertEquals("4.1 MB / ?", formatByteProgress(4_100_000L, null))
    }

    @Test
    fun `a zero total is reported as unknown`() {
        assertEquals("0 B / ?", formatByteProgress(0L, null))
    }
}

class DownloadSizeTest {
    @Test
    fun `a non positive total is normalised to an unknown total`() {
        assertEquals(null, downloadSizeOf(bytesDownloaded = 10L, bytesTotal = 0L).bytesTotal)
        assertEquals(null, downloadSizeOf(bytesDownloaded = 10L, bytesTotal = -1L).bytesTotal)
    }

    @Test
    fun `an unknown total yields no fraction rather than zero percent`() {
        val size = downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = -1L)

        assertEquals(null, size.fraction)
        assertEquals(false, size.isTotalKnown)
    }

    @Test
    fun `a known total derives the fraction from the byte counts`() {
        val size = downloadSizeOf(bytesDownloaded = 5L, bytesTotal = 10L)

        assertEquals(0.5f, size.fraction)
        assertEquals(true, size.isTotalKnown)
    }

    @Test
    fun `a fraction never exceeds one when more bytes arrive than announced`() {
        assertEquals(1f, downloadSizeOf(bytesDownloaded = 20L, bytesTotal = 10L).fraction)
    }

    @Test
    fun `the label pairs downloaded bytes with the total`() {
        assertEquals(
            "4.1 MB / 12.1 MB",
            downloadSizeOf(bytesDownloaded = 4_100_000L, bytesTotal = 12_100_000L).label,
        )
    }
}
