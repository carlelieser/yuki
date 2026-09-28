package app.yuki.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val CALLBACK = "https://yukistore.org/auth/mobile/callback"

class BrowserAuthResultTest {
    @Test
    fun `a ticket signs the app in`() {
        assertEquals(
            BrowserAuthResult.SignedIn(ticket = "abc", state = "s1"),
            parseBrowserAuthResult("$CALLBACK?token=abc&state=s1"),
        )
    }

    @Test
    fun `a linked result is recognised`() {
        assertEquals(
            BrowserAuthResult.Linked(state = "s1"),
            parseBrowserAuthResult("$CALLBACK?result=linked&state=s1"),
        )
    }

    @Test
    fun `an error wins over a ticket`() {
        assertEquals(
            BrowserAuthResult.Failed(code = "access_denied", state = "s1"),
            parseBrowserAuthResult("$CALLBACK?token=abc&error=access_denied&state=s1"),
        )
    }

    @Test
    fun `encoded values are decoded`() {
        assertEquals(
            BrowserAuthResult.SignedIn(ticket = "a b", state = "s-1_"),
            parseBrowserAuthResult("$CALLBACK?token=a%20b&state=s-1_"),
        )
    }

    @Test
    fun `a result without state is ignored`() {
        assertNull(parseBrowserAuthResult("$CALLBACK?token=abc"))
    }

    @Test
    fun `unrelated links are ignored`() {
        assertNull(parseBrowserAuthResult(null))
        assertNull(parseBrowserAuthResult("$CALLBACK"))
        assertNull(parseBrowserAuthResult("not a url"))
    }
}
