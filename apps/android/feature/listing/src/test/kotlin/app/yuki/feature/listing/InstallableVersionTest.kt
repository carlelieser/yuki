package app.yuki.feature.listing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallableVersionTest {
    @Test
    fun `picks the newest version when it is installable`() {
        val listing = detail(versions = listOf(version("v3.0.0"), version("v2.0.0")))

        assertEquals("v3.0.0", installableVersion(listing)?.tag)
    }

    @Test
    fun `skips prereleases in favour of the newest stable release`() {
        val versions = listOf(
            version("v4.0.0-beta.1", isPrerelease = true),
            version("v3.0.0"),
        )

        assertEquals("v3.0.0", installableVersion(detail(versions = versions))?.tag)
    }

    @Test
    fun `skips versions without a download url`() {
        val versions = listOf(
            version("v4.0.0", downloadUrl = null),
            version("v3.0.0"),
        )

        assertEquals("v3.0.0", installableVersion(detail(versions = versions))?.tag)
    }

    @Test
    fun `skips a prerelease that also has a download url`() {
        val versions = listOf(
            version("v5.0.0-rc.1", isPrerelease = true),
            version("v4.0.0", downloadUrl = null),
            version("v3.0.0"),
        )

        assertEquals("v3.0.0", installableVersion(detail(versions = versions))?.tag)
    }

    @Test
    fun `returns null when every version is a prerelease`() {
        val versions = listOf(
            version("v2.0.0-beta", isPrerelease = true),
            version("v1.0.0-alpha", isPrerelease = true),
        )

        assertNull(installableVersion(detail(versions = versions)))
    }

    @Test
    fun `returns null when no version carries a download url`() {
        val versions = listOf(version("v2.0.0", downloadUrl = null))

        assertNull(installableVersion(detail(versions = versions)))
    }

    @Test
    fun `returns null when the version list is empty`() {
        assertNull(installableVersion(detail(versions = emptyList())))
    }

    @Test
    fun `exposes the download url of the selected version to the installer`() {
        val request = ListingInstallRequest(detail = detail(), version = version("v2.0.0"))

        assertEquals("https://cdn.test/v2.0.0.apk", request.downloadUrl)
    }

    @Test
    fun `marks a listing with an eligible version as installable`() {
        assertTrue(detail().toUiModel().isInstallable)
    }

    @Test
    fun `marks a listing without an eligible version as not installable`() {
        assertFalse(detail(versions = emptyList()).toUiModel().isInstallable)
    }
}
