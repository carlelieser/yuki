package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val REPO_ID = 1_234_567_890L

private fun installedApp(versionTag: String) = InstalledApp(
    githubRepoId = REPO_ID,
    packageName = "app.example",
    slug = "example",
    title = "Example",
    iconUrl = null,
    versionTag = versionTag,
)

private fun detectedApp(versionName: String) = InstalledApp(
    githubRepoId = REPO_ID,
    packageName = "app.example",
    slug = "example",
    title = "Example",
    iconUrl = null,
    versionTag = versionName,
    source = InstallSource.DETECTED,
)

private fun version(
    tag: String,
    downloadUrl: String? = "https://example.test/$tag.apk",
    isPrerelease: Boolean = false,
) = ListingVersion(
    tag = tag,
    name = tag,
    downloadUrl = downloadUrl,
    assetName = "app.apk",
    isPrerelease = isPrerelease,
    publishedAt = null,
)

private fun detail(versions: List<ListingVersion>) = ListingDetail(
    summary = ListingSummary(
        id = "id",
        githubRepoId = REPO_ID,
        slug = "example",
        title = "Example",
        author = "author",
        description = null,
        iconUrl = null,
        bannerUrl = null,
        category = null,
        stars = 0,
    ),
    links = ListingLinks("https://example.test", "https://example.test/repo", null),
    license = null,
    isArchived = false,
    screenshots = emptyList(),
    versions = versions,
)

class FindUpdatesTest {
    @Test
    fun `picks the newest eligible version`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("1.3.0"), version("1.5.0"), version("1.4.0"))))

        val updates = findUpdates(listOf(installedApp("1.2.0")), listings, includePrereleases = false)

        assertEquals(listOf("1.5.0"), updates.map { update -> update.version.tag })
    }

    @Test
    fun `skips versions without a download url`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("2.0.0", downloadUrl = null), version("1.5.0"))))

        val updates = findUpdates(listOf(installedApp("1.2.0")), listings, includePrereleases = false)

        assertEquals(listOf("1.5.0"), updates.map { update -> update.version.tag })
    }

    @Test
    fun `filters prereleases unless enabled`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("2.0.0-beta.1", isPrerelease = true))))

        val withoutPrereleases =
            findUpdates(listOf(installedApp("1.2.0")), listings, includePrereleases = false)
        val withPrereleases =
            findUpdates(listOf(installedApp("1.2.0")), listings, includePrereleases = true)

        assertTrue(withoutPrereleases.isEmpty())
        assertEquals(listOf("2.0.0-beta.1"), withPrereleases.map { update -> update.version.tag })
    }

    @Test
    fun `reports nothing when the installed tag is current`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("1.2.0"))))

        val updates = findUpdates(listOf(installedApp("1.2.0")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `ignores installs with no matching listing`() {
        val updates = findUpdates(listOf(installedApp("1.2.0")), emptyMap(), includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `never reports an update for an unparseable installed tag`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("2.0.0"))))

        val updates = findUpdates(listOf(installedApp("nightly")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `updates a detected app once its version matches a release tag`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("v1.6.17"), version("v1.7.0"))))

        val updates = findUpdates(listOf(detectedApp("1.6.17")), listings, includePrereleases = false)

        assertEquals(listOf("v1.7.0"), updates.map { update -> update.version.tag })
    }

    @Test
    fun `reports nothing for a detected app that is already on the newest release`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("v1.6.17"))))

        val updates = findUpdates(listOf(detectedApp("1.6.17")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `never updates a detected app whose version matches no release`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("v2.0.0"), version("v3.0.0"))))

        val updates = findUpdates(listOf(detectedApp("1.6.17")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `never updates a detected app whose version matches more than one release`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("v1.6.17"), version("1.6.17"))))

        val updates = findUpdates(listOf(detectedApp("1.6.17")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `never updates a detected app that reports no version at all`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("v2.0.0"))))

        val updates = findUpdates(listOf(detectedApp("")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }

    @Test
    fun `never downgrades a detected app to an older release`() {
        val listings = mapOf(REPO_ID to detail(listOf(version("v1.0.0"), version("v1.6.17"))))

        val updates = findUpdates(listOf(detectedApp("1.6.17")), listings, includePrereleases = false)

        assertTrue(updates.isEmpty())
    }
}
