package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun version(tag: String) = ListingVersion(
    tag = tag,
    name = tag,
    downloadUrl = "https://example.test/$tag.apk",
    assetName = "app.apk",
    isPrerelease = false,
    publishedAt = null,
)

class SelfListingTest {
    @Test
    fun `matches a version name against a prefixed release tag`() {
        val versions = listOf(version("android-v1.0.1"), version("android-v1.0.0"))

        assertEquals("android-v1.0.0", resolveInstalledTag("1.0.0", versions))
    }

    @Test
    fun `matches a tag that carries no prefix`() {
        assertEquals("1.0.0", resolveInstalledTag("1.0.0", listOf(version("1.0.0"))))
    }

    @Test
    fun `returns null when no published tag matches the running version`() {
        val versions = listOf(version("android-v1.0.0"), version("android-v1.0.1"))

        assertNull(resolveInstalledTag("9.9.9", versions))
    }

    @Test
    fun `returns null for an unparseable version name`() {
        assertNull(resolveInstalledTag("nightly", listOf(version("android-v1.0.0"))))
    }

    @Test
    fun `returns null when the listing has no versions`() {
        assertNull(resolveInstalledTag("1.0.0", emptyList()))
    }

    @Test
    fun `picks the equal version rather than the newest`() {
        val versions = listOf(version("android-v2.0.0"), version("android-v1.0.0"))

        assertEquals("android-v1.0.0", resolveInstalledTag("1.0.0", versions))
    }

    @Test
    fun `distinguishes a prerelease from its final release`() {
        val versions = listOf(version("android-v1.0.0-rc1"), version("android-v1.0.0"))

        assertEquals("android-v1.0.0-rc1", resolveInstalledTag("1.0.0-rc1", versions))
        assertEquals("android-v1.0.0", resolveInstalledTag("1.0.0", versions))
    }
}
