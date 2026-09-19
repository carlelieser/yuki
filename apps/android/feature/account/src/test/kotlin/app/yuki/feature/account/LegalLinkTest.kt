package app.yuki.feature.account

import org.junit.Assert.assertEquals
import org.junit.Test

class LegalLinkTest {
    @Test
    fun `a base url with a trailing slash does not produce a double slash`() {
        assertEquals("https://yuki.test/terms", legalUrl("https://yuki.test/", "terms"))
    }

    @Test
    fun `a base url without a trailing slash still resolves`() {
        assertEquals("https://yuki.test/privacy", legalUrl("https://yuki.test", "privacy"))
    }
}
