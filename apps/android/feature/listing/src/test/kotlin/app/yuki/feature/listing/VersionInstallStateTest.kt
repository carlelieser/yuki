package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionInstallStateTest {
    @Test
    fun `shows the live state on the version being installed`() {
        val downloading = InstallState.Downloading(HALF_DOWNLOADED)
        val status = ListingInstallStatus(state = downloading, versionTag = "v2.0.0")

        val row = versionInstallState(version = version("v2.0.0"), status = status)

        assertEquals(downloading, row.state)
        assertTrue(row.isEnabled)
    }

    @Test
    fun `disables other versions while one is downloading`() {
        val downloading = InstallState.Downloading(HALF_DOWNLOADED)
        val status = ListingInstallStatus(state = downloading, versionTag = "v2.0.0")

        val row = versionInstallState(version = version("v1.0.0"), status = status)

        assertEquals(InstallState.NotInstalled, row.state)
        assertFalse(row.isEnabled)
    }

    @Test
    fun `disables other versions while one awaits user confirmation`() {
        val status = ListingInstallStatus(
            state = InstallState.PendingUserAction,
            versionTag = "v2.0.0",
        )

        assertFalse(versionInstallState(version("v1.0.0"), status).isEnabled)
    }

    @Test
    fun `disables other versions while one is being installed`() {
        val status = ListingInstallStatus(
            state = InstallState.Installing,
            versionTag = "v2.0.0",
        )

        assertFalse(versionInstallState(version("v1.0.0"), status).isEnabled)
    }

    @Test
    fun `keeps other versions enabled once an install settles`() {
        val status = ListingInstallStatus(
            state = InstallState.Installed("v2.0.0"),
            versionTag = "v2.0.0",
        )

        val row = versionInstallState(version = version("v1.0.0"), status = status)

        assertEquals(InstallState.NotInstalled, row.state)
        assertTrue(row.isEnabled)
    }

    @Test
    fun `offers every version when nothing is installing`() {
        val status = ListingInstallStatus(
            state = InstallState.NotInstalled,
            versionTag = null,
        )

        val row = versionInstallState(version = version("v1.0.0"), status = status)

        assertEquals(InstallState.NotInstalled, row.state)
        assertTrue(row.isEnabled)
    }

    @Test
    fun `treats a version carrying a download url as having an asset`() {
        assertTrue(version("v2.0.0").toInstallable() != null)
    }

    @Test
    fun `treats a version without a download url as having no asset`() {
        assertNull(version("v2.0.0", downloadUrl = null).toInstallable())
    }

    @Test
    fun `treats a prerelease with an asset as installable from its row`() {
        assertTrue(version("v2.0.0-rc", isPrerelease = true).toInstallable() != null)
    }
}

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)
