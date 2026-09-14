package app.yuki.feature.updates

import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.SelfListing
import java.io.IOException
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

private val CLOCK = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC)

private fun selfListing(versionName: String) = SelfListing(
    githubRepoId = SELF_REPO_ID,
    slug = SELF_SLUG,
    packageName = "app.yuki",
    versionName = versionName,
    versionCode = 289L,
)

private fun reconciler(
    store: FakeInstallStore,
    details: Map<String, Result<ListingDetail>>,
    versionName: String,
) = SelfInstallReconciler(
    store = store,
    repository = FakeListingRepository(details),
    self = selfListing(versionName),
    clock = CLOCK,
)

class SelfInstallReconcilerTest {
    @Test
    fun `records the running build against its published tag`() = runTest {
        val store = FakeInstallStore(emptyList())
        val detail = listingDetail(
            SELF_REPO_ID,
            SELF_SLUG,
            listOf(version("android-v1.0.1"), version("android-v1.0.0")),
        )

        val result = reconciler(store, mapOf(SELF_SLUG to Result.success(detail)), "1.0.0")
            .reconcile()

        assertEquals("android-v1.0.0", result.getOrNull())
        assertEquals(
            listOf(SELF_REPO_ID to "android-v1.0.0"),
            store.installs().map { app -> app.githubRepoId to app.versionTag },
        )
    }

    @Test
    fun `writes nothing when the listing cannot be fetched`() = runTest {
        val store = FakeInstallStore(emptyList())

        val result = reconciler(store, mapOf(SELF_SLUG to Result.failure(IOException("offline"))), "1.0.0")
            .reconcile()

        assertTrue(result.isFailure)
        assertTrue(store.installs().isEmpty())
    }

    @Test
    fun `leaves an existing row untouched when the listing cannot be fetched`() = runTest {
        val existing = InstalledApp(
            githubRepoId = SELF_REPO_ID,
            packageName = "app.yuki",
            slug = SELF_SLUG,
            title = "Yuki",
            iconUrl = null,
            versionTag = "android-v1.0.0",
        )
        val store = FakeInstallStore(listOf(existing))

        reconciler(store, mapOf(SELF_SLUG to Result.failure(IOException("offline"))), "1.0.0")
            .reconcile()

        assertEquals(listOf(existing), store.installs())
    }

    @Test
    fun `writes nothing when the running version matches no published tag`() = runTest {
        val store = FakeInstallStore(emptyList())
        val detail = listingDetail(SELF_REPO_ID, SELF_SLUG, listOf(version("android-v1.0.0")))

        val result = reconciler(store, mapOf(SELF_SLUG to Result.success(detail)), "9.9.9")
            .reconcile()

        assertNull(result.getOrNull())
        assertTrue(store.installs().isEmpty())
    }

    @Test
    fun `writes nothing for an unparseable running version`() = runTest {
        val store = FakeInstallStore(emptyList())
        val detail = listingDetail(SELF_REPO_ID, SELF_SLUG, listOf(version("android-v1.0.0")))

        val result = reconciler(store, mapOf(SELF_SLUG to Result.success(detail)), "dev")
            .reconcile()

        assertNull(result.getOrNull())
        assertTrue(store.installs().isEmpty())
    }

    @Test
    fun `advances the recorded tag after a self update`() = runTest {
        val store = FakeInstallStore(
            listOf(
                InstalledApp(
                    githubRepoId = SELF_REPO_ID,
                    packageName = "app.yuki",
                    slug = SELF_SLUG,
                    title = "Yuki",
                    iconUrl = null,
                    versionTag = "android-v1.0.0",
                ),
            ),
        )
        val detail = listingDetail(
            SELF_REPO_ID,
            SELF_SLUG,
            listOf(version("android-v1.0.1"), version("android-v1.0.0")),
        )

        reconciler(store, mapOf(SELF_SLUG to Result.success(detail)), "1.0.1").reconcile()

        assertEquals(
            listOf("android-v1.0.1"),
            store.installs().map(InstalledApp::versionTag),
        )
    }

    @Test
    fun `keeps a single row across repeated reconciles`() = runTest {
        val store = FakeInstallStore(emptyList())
        val detail = listingDetail(SELF_REPO_ID, SELF_SLUG, listOf(version("android-v1.0.0")))
        val subject = reconciler(store, mapOf(SELF_SLUG to Result.success(detail)), "1.0.0")

        subject.reconcile()
        subject.reconcile()

        assertEquals(1, store.installs().size)
    }
}
