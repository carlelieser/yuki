package app.yuki.core.installer

import androidx.work.Data
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class InstallWorkRequestTest {
    @Test
    fun `an install request survives a round trip through work data`() {
        val request = testRequest(githubRepoId = 77L, versionTag = "v3.1.0")

        assertEquals(request, request.toWorkData().toInstallRequest())
    }

    @Test
    fun `a request without an asset name round trips its null`() {
        val request = InstallRequest(
            target = InstallTarget(1L, "acme", "Acme", iconUrl = null),
            source = InstallSource("https://example.test/a.apk", "v1", assetName = null),
        )

        val restored = request.toWorkData().toInstallRequest()

        assertEquals(null, restored.source.assetName)
        assertEquals(null, restored.target.iconUrl)
    }

    @Test
    fun `work data missing the download url names the missing value`() {
        val incomplete = Data.Builder()
            .putLong(InstallWorkKeys.GITHUB_REPO_ID, 1L)
            .putString(InstallWorkKeys.SLUG, "acme")
            .putString(InstallWorkKeys.TITLE, "Acme")
            .putString(InstallWorkKeys.VERSION_TAG, "v1")
            .build()

        val error = assertThrows(IllegalArgumentException::class.java) {
            incomplete.toInstallRequest()
        }

        assertEquals(true, error.message.orEmpty().contains(InstallWorkKeys.DOWNLOAD_URL))
    }

    @Test
    fun `work data missing the repo id names the missing value`() {
        val error = assertThrows(IllegalStateException::class.java) {
            Data.Builder().putString(InstallWorkKeys.SLUG, "acme").build().toInstallRequest()
        }

        assertEquals(true, error.message.orEmpty().contains(InstallWorkKeys.GITHUB_REPO_ID))
    }

    @Test
    fun `each repo id gets its own unique work name so installs do not collide`() {
        assertEquals(installWorkName(1L), installWorkName(1L))
        assertEquals(false, installWorkName(1L) == installWorkName(2L))
    }
}
