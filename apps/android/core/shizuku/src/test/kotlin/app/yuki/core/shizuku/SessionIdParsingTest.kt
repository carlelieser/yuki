package app.yuki.core.shizuku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionIdParsingTest {
    @Test
    fun `reads the session id from a successful create`() {
        assertEquals(731, parseSessionId("Success: created install session [731]"))
    }

    @Test
    fun `tolerates surrounding whitespace and newlines`() {
        assertEquals(4, parseSessionId("\nSuccess: created install session [4]\n"))
    }

    @Test
    fun `refuses to guess when no session id is present`() {
        val error = assertThrows(PrivilegedInstallException::class.java) {
            parseSessionId("Error: java.lang.SecurityException")
        }

        assertTrue(error.message.orEmpty().contains("java.lang.SecurityException"))
    }
}
