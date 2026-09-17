package app.yuki.feature.listing

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenInObtainiumTest {
    @Test
    fun `builds an obtainium add link from a repository url`() {
        assertEquals(
            "obtainium://add/https://github.com/nightsky/aurora",
            obtainiumAddUrl("https://github.com/nightsky/aurora"),
        )
    }

    @Test
    fun `hands obtainium the repository url unencoded`() {
        val url = obtainiumAddUrl("https://github.com/nightsky/aurora")

        assertEquals("https://github.com/nightsky/aurora", url.substringAfter("obtainium://add/"))
    }
}
