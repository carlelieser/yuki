package app.yuki.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.model.InstalledApp
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallStoreTest {
    private lateinit var database: YukiDatabase
    private lateinit var store: InstallStore

    @Before
    fun createStore() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, YukiDatabase::class.java).build()
        store = RoomInstallStore(database.installDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun recordsAnInstallAsAnInstalledApp() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))

        assertEquals(listOf(TERMUX), store.installs())
    }

    @Test
    fun upsertReplacesTheRowForTheSameRepoId() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))
        val upgraded = TERMUX.copy(versionTag = "v0.119.0")

        store.record(recordingFor(upgraded, installedAt = EPOCH.plusSeconds(60), versionCode = 119))

        assertEquals(listOf(upgraded), store.installs())
    }

    @Test
    fun ordersInstallsNewestFirst() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))
        store.record(recordingFor(AURORA, installedAt = EPOCH.plusSeconds(60)))

        assertEquals(listOf(AURORA, TERMUX), store.observeInstalls().first())
    }

    @Test
    fun exposesTheRecordedPackageNameForARepoId() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))

        assertEquals(TERMUX.packageName, store.packageNameOf(TERMUX.githubRepoId))
        assertNull(store.packageNameOf(AURORA.githubRepoId))
    }

    @Test
    fun forgettingUninstalledPackagesDropsOnlyThoseRows() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))
        store.record(recordingFor(AURORA, installedAt = EPOCH.plusSeconds(60)))

        store.forgetPackages(listOf(AURORA.packageName))

        assertEquals(listOf(TERMUX), store.installs())
    }

    @Test
    fun forgettingNoPackagesKeepsEveryRow() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))

        store.forgetPackages(emptyList())

        assertEquals(listOf(TERMUX), store.installs())
    }

    @Test
    fun forgettingByRepoIdDropsTheRow() = runTest {
        store.record(recordingFor(TERMUX, installedAt = EPOCH))

        store.forget(TERMUX.githubRepoId)

        assertEquals(emptyList<InstalledApp>(), store.installs())
    }
}

private val EPOCH: Instant = Instant.ofEpochMilli(1_700_000_000_000)

private val TERMUX = InstalledApp(
    githubRepoId = 1_234L,
    packageName = "com.termux",
    slug = "termux",
    title = "Termux",
    iconUrl = "https://yuki.app/termux.png",
    versionTag = "v0.118.0",
)

private val AURORA = InstalledApp(
    githubRepoId = 5_678L,
    packageName = "com.aurora.store",
    slug = "aurora-store",
    title = "Aurora Store",
    iconUrl = null,
    versionTag = "4.6.4",
)

private fun recordingFor(
    app: InstalledApp,
    installedAt: Instant,
    versionCode: Long? = null,
): InstallRecording = InstallRecording(app = app, versionCode = versionCode, installedAt = installedAt)
