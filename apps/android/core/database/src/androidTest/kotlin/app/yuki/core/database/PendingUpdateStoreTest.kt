package app.yuki.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.model.AvailableUpdate
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingVersion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingUpdateStoreTest {
    private lateinit var database: YukiDatabase
    private lateinit var store: PendingUpdateStore

    @Before
    fun createStore() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, YukiDatabase::class.java).build()
        store = RoomPendingUpdateStore(database.pendingUpdateDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun recordsTheUpdatesFoundByACheck() = runTest {
        store.replace(checkedWith(TERMUX to "v2"), isSeen = false)

        assertEquals(listOf(PendingUpdate(TERMUX.githubRepoId, "v2", false)), store.observe().first())
    }

    @Test
    fun aCheckedAppWithNoUpdateLosesItsPendingUpdate() = runTest {
        store.replace(checkedWith(TERMUX to "v2"), isSeen = false)

        store.replace(CheckedUpdates(setOf(TERMUX.githubRepoId), emptyList()), isSeen = false)

        assertEquals(emptyList<PendingUpdate>(), store.observe().first())
    }

    @Test
    fun anAppThatCouldNotBeCheckedKeepsItsPendingUpdate() = runTest {
        store.replace(checkedWith(TERMUX to "v2", AURORA to "5.0.0"), isSeen = false)

        store.replace(checkedWith(AURORA to "5.0.0"), isSeen = false)

        assertEquals(
            listOf(TERMUX.githubRepoId, AURORA.githubRepoId),
            store.observe().first().map(PendingUpdate::githubRepoId).sorted(),
        )
    }

    @Test
    fun seenUpdatesAreNotReportedAsUnnotified() = runTest {
        store.replace(checkedWith(TERMUX to "v2"), isSeen = true)

        assertEquals(emptyList<PendingUpdate>(), store.unnotified())
    }

    @Test
    fun aNotifiedUpdateStaysNotifiedWhenFoundAgain() = runTest {
        store.replace(checkedWith(TERMUX to "v2"), isSeen = false)
        store.markNotified(listOf(TERMUX.githubRepoId))

        store.replace(checkedWith(TERMUX to "v2"), isSeen = false)

        assertEquals(emptyList<PendingUpdate>(), store.unnotified())
    }

    @Test
    fun aNewerVersionIsUnnotifiedEvenAfterAnEarlierOneWasNotified() = runTest {
        store.replace(checkedWith(TERMUX to "v2"), isSeen = false)
        store.markNotified(listOf(TERMUX.githubRepoId))

        store.replace(checkedWith(TERMUX to "v3"), isSeen = false)

        assertEquals(listOf(PendingUpdate(TERMUX.githubRepoId, "v3", false)), store.unnotified())
    }
}

private val TERMUX = InstalledApp(
    githubRepoId = 1_234L,
    packageName = "com.termux",
    slug = "termux",
    title = "Termux",
    iconUrl = null,
    versionTag = "v1",
)

private val AURORA = InstalledApp(
    githubRepoId = 5_678L,
    packageName = "com.aurora.store",
    slug = "aurora-store",
    title = "Aurora Store",
    iconUrl = null,
    versionTag = "4.6.4",
)

private fun checkedWith(vararg found: Pair<InstalledApp, String>): CheckedUpdates = CheckedUpdates(
    githubRepoIds = found.map { (app, _) -> app.githubRepoId }.toSet(),
    updates = found.map { (app, tag) -> AvailableUpdate(app, versionTagged(tag)) },
)

private fun versionTagged(tag: String): ListingVersion = ListingVersion(
    tag = tag,
    name = null,
    downloadUrl = "https://yuki.app/$tag.apk",
    assetName = null,
    isPrerelease = false,
    publishedAt = null,
)
