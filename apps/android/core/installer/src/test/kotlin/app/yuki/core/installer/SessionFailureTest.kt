package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionFailureTest {
    @Test
    fun `a session error that is not about space is a failed session`() {
        assertEquals(
            InstallFailure.SessionFailed,
            sessionFailure(IOException("Session relinquished")),
        )
    }

    @Test
    fun `running out of space is insufficient storage`() {
        assertEquals(
            InstallFailure.InsufficientStorage,
            sessionFailure(IOException("write failed: ENOSPC (No space left on device)")),
        )
    }

    @Test
    fun `running out of space deeper in the cause chain is insufficient storage`() {
        val error = IOException("Failed to write", IOException("ENOSPC (No space left on device)"))

        assertEquals(InstallFailure.InsufficientStorage, sessionFailure(error))
    }
}
