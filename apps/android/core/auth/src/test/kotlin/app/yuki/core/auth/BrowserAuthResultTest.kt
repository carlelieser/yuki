package app.yuki.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val CALLBACK = "https://yukistore.org/auth/mobile/callback"

class BrowserAuthResultTest {
    private fun parse(url: String?): BrowserAuthResult? {
        val signIn = url?.replace("callback?", "callback?flow=signin&")
        return parseBrowserAuthReturn(signIn)?.result
    }

    @Test
    fun `the flow says which screen the result belongs to`() {
        assertEquals(
            BrowserAuthPurpose.Link,
            parseBrowserAuthReturn("$CALLBACK?flow=link&result=linked&state=s1")?.purpose,
        )
    }

    @Test
    fun `a result without a flow is ignored`() {
        assertNull(parseBrowserAuthReturn("$CALLBACK?token=abc&state=s1"))
    }

    @Test
    fun `a ticket signs the app in`() {
        assertEquals(
            BrowserAuthResult.SignedIn(ticket = "abc", state = "s1"),
            parse("$CALLBACK?token=abc&state=s1"),
        )
    }

    @Test
    fun `a linked result is recognised`() {
        assertEquals(
            BrowserAuthResult.Linked(state = "s1"),
            parse("$CALLBACK?result=linked&state=s1"),
        )
    }

    @Test
    fun `an error wins over a ticket`() {
        assertEquals(
            BrowserAuthResult.Failed(code = "access_denied", state = "s1"),
            parse("$CALLBACK?token=abc&error=access_denied&state=s1"),
        )
    }

    @Test
    fun `encoded values are decoded`() {
        assertEquals(
            BrowserAuthResult.SignedIn(ticket = "a b", state = "s-1_"),
            parse("$CALLBACK?token=a%20b&state=s-1_"),
        )
    }

    @Test
    fun `a result without state is ignored`() {
        assertNull(parse("$CALLBACK?token=abc"))
    }

    @Test
    fun `unrelated links are ignored`() {
        assertNull(parse(null))
        assertNull(parse("$CALLBACK"))
        assertNull(parse("not a url"))
    }
}
