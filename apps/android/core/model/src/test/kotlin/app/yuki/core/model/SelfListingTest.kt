package app.yuki.core.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun selfListing(releaseTag: String) = SelfListing(
    githubRepoId = 1L,
    slug = "carlelieser-yuki",
    packageName = "app.yuki",
    title = "Yuki",
    iconUrl = "https://example.com/icon.png",
    releaseTag = releaseTag,
    versionCode = 1L,
)

class SelfListingTest {
    @Test
    fun `the running release has landed`() {
        assertTrue(selfListing("android-v1.7.2").hasLanded("android-v1.7.2"))
    }

    @Test
    fun `an older release has landed because the running build replaced it`() {
        assertTrue(selfListing("android-v1.7.2").hasLanded("android-v1.7.1"))
    }

    @Test
    fun `a newer release has not landed`() {
        assertFalse(selfListing("android-v1.7.2").hasLanded("android-v1.8.0"))
    }

    @Test
    fun `nothing has landed on a build that came from no release`() {
        assertFalse(selfListing("").hasLanded("android-v1.7.2"))
    }
}
