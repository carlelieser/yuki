package app.yuki.feature.updates

import app.yuki.core.model.InstalledApp
import app.yuki.core.model.SelfListing
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val SELF_REPO_ID = 1_359_590_051L
private const val SELF_SLUG = "carlelieser-yuki"
private const val SELF_ICON = "https://example.com/icon.png"

private val CLOCK = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)

private fun selfListing(releaseTag: String) = SelfListing(
    githubRepoId = SELF_REPO_ID,
    slug = SELF_SLUG,
    packageName = "app.yuki",
    title = "Yuki",
    iconUrl = SELF_ICON,
    releaseTag = releaseTag,
    versionCode = 318L,
)

private fun reconciler(store: FakeInstallStore, releaseTag: String) = SelfInstallReconciler(
    store = store,
    self = selfListing(releaseTag),
    clock = CLOCK,
)

class SelfInstallReconcilerTest {
    @Test
    fun `records the running build against the tag it was built from`() = runTest {
        val store = FakeInstallStore(emptyList())

        val tag = reconciler(store, "android-v1.2.0").reconcile()

        assertEquals("android-v1.2.0", tag)
        assertEquals(
            listOf(SELF_REPO_ID to "android-v1.2.0"),
            store.installs().map { app -> app.githubRepoId to app.versionTag },
        )
    }

    @Test
    fun `records the build even when the catalog has not published the release yet`() = runTest {
        val store = FakeInstallStore(emptyList())

        val tag = reconciler(store, "android-v9.9.9").reconcile()

        assertEquals("android-v9.9.9", tag)
        assertEquals(1, store.installs().size)
    }

    @Test
    fun `advances the recorded tag when the build is upgraded`() = runTest {
        val store = FakeInstallStore(
            listOf(
                InstalledApp(
                    githubRepoId = SELF_REPO_ID,
                    packageName = "app.yuki",
                    slug = SELF_SLUG,
                    title = "Yuki",
                    iconUrl = SELF_ICON,
                    versionTag = "android-v1.1.0",
                ),
            ),
        )

        reconciler(store, "android-v1.2.0").reconcile()

        assertEquals(
            listOf(SELF_REPO_ID to "android-v1.2.0"),
            store.installs().map { app -> app.githubRepoId to app.versionTag },
        )
    }

    @Test
    fun `writes nothing for a build that came from no release`() = runTest {
        val store = FakeInstallStore(emptyList())

        val tag = reconciler(store, "").reconcile()

        assertNull(tag)
        assertTrue(store.installs().isEmpty())
    }
}
