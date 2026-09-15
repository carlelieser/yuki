package app.yuki.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.model.InstallSource
import app.yuki.core.model.InstalledApp
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FreshInstallSourceTest {
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
    fun aYukiInstallRoundTripsItsSource() = runTest {
        store.record(recordingFor(InstallSource.YUKI))

        assertEquals(InstallSource.YUKI, store.installs().single().source)
    }

    @Test
    fun aDetectedInstallRoundTripsItsSource() = runTest {
        store.record(recordingFor(InstallSource.DETECTED))

        val stored = store.installs().single()
        assertEquals(InstallSource.DETECTED, stored.source)
        assertEquals(true, stored.isDetected)
    }
}

private fun recordingFor(source: InstallSource) = InstallRecording(
    app = InstalledApp(
        githubRepoId = 11L,
        packageName = "com.probe",
        slug = "probe",
        title = "Probe",
        iconUrl = null,
        versionTag = "1.0.0",
        source = source,
    ),
    versionCode = 1L,
    installedAt = Instant.ofEpochMilli(1_700_000_000_000),
)
