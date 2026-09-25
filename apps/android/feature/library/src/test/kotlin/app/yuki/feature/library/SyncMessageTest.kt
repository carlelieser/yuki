package app.yuki.feature.library

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncMessageTest {
    @Test
    fun `failed uploads are reported alongside the added count`() {
        assertEquals(
            SyncMessage.Partial(uploaded = 2, failed = 1),
            syncMessage(LibrarySyncResult(uploaded = 2, failed = 1)),
        )
    }

    @Test
    fun `added apps are counted`() {
        assertEquals(SyncMessage.Added(uploaded = 3), syncMessage(LibrarySyncResult(uploaded = 3)))
    }

    @Test
    fun `nothing to upload reads as up to date`() {
        assertEquals(SyncMessage.UpToDate, syncMessage(LibrarySyncResult()))
    }
}
