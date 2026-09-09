package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseVersionTest {
    @Test
    fun `reads a plain semantic version`() {
        assertEquals(ParsedVersion(listOf(1, 2, 3), null), parseVersion("1.2.3"))
    }

    @Test
    fun `ignores a leading v and other prefixes`() {
        assertEquals(ParsedVersion(listOf(1, 2, 3), null), parseVersion("v1.2.3"))
        assertEquals(ParsedVersion(listOf(2, 0), null), parseVersion("release-2.0"))
    }

    @Test
    fun `reads a prerelease suffix`() {
        assertEquals(
            ParsedVersion(listOf(1, 2, 3), listOf("beta", "1")),
            parseVersion("1.2.3-beta.1"),
        )
    }

    @Test
    fun `discards build metadata`() {
        assertEquals(ParsedVersion(listOf(1, 2, 3), null), parseVersion("1.2.3+build.5"))
    }

    @Test
    fun `returns null for tags carrying no version`() {
        assertNull(parseVersion("latest"))
        assertNull(parseVersion(""))
    }
}

class IsNewerTagTest {
    @Test
    fun `detects a higher release`() {
        assertTrue(isNewerTag("1.2.4", "1.2.3"))
        assertTrue(isNewerTag("2.0.0", "1.9.9"))
    }

    @Test
    fun `rejects the same or an older release`() {
        assertFalse(isNewerTag("1.2.3", "1.2.3"))
        assertFalse(isNewerTag("1.2.2", "1.2.3"))
    }

    @Test
    fun `compares numeric parts rather than strings`() {
        assertTrue(isNewerTag("1.10.0", "1.9.0"))
        assertFalse(isNewerTag("1.9.0", "1.10.0"))
    }

    @Test
    fun `treats missing trailing parts as zero`() {
        assertFalse(isNewerTag("1.2", "1.2.0"))
        assertTrue(isNewerTag("1.2.1", "1.2"))
    }

    @Test
    fun `ranks a release above its own prereleases`() {
        assertTrue(isNewerTag("1.2.3", "1.2.3-beta.1"))
        assertFalse(isNewerTag("1.2.3-beta.1", "1.2.3"))
    }

    @Test
    fun `orders prereleases against each other`() {
        assertTrue(isNewerTag("1.2.3-beta.2", "1.2.3-beta.1"))
        assertTrue(isNewerTag("1.2.3-rc.1", "1.2.3-beta.1"))
        assertFalse(isNewerTag("1.2.3-beta.1", "1.2.3-rc.1"))
    }

    @Test
    fun `compares tags written with differing prefixes`() {
        assertTrue(isNewerTag("v1.2.4", "1.2.3"))
    }

    @Test
    fun `never reports an update when either tag is unparseable`() {
        assertFalse(isNewerTag("latest", "1.2.3"))
        assertFalse(isNewerTag("1.2.4", "nightly"))
        assertFalse(isNewerTag("nightly", "latest"))
    }

    @Test
    fun `reports no update for identical unparseable tags`() {
        assertFalse(isNewerTag("nightly", "nightly"))
    }
}
