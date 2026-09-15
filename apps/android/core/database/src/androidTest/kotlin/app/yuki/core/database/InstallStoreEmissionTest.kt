package app.yuki.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.model.InstalledApp
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallStoreEmissionTest {
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
    fun writingWhileObservingInstallsFeedsBackIntoTheObserver() {
        val recording = recordingFor(PROBE)
        runBlocking { store.record(recording) }

        val emissions = AtomicInteger(0)
        val writes = AtomicInteger(0)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        store.observeInstalls().onEach {
            emissions.incrementAndGet()
            if (writes.get() < WRITE_LIMIT) {
                writes.incrementAndGet()
                store.record(recording)
            }
        }.launchIn(scope)

        Thread.sleep(SETTLE_MILLIS)
        scope.cancel()

        assertTrue(
            "an identical upsert re-emitted ${emissions.get()} times for ${writes.get()} writes",
            emissions.get() > writes.get(),
        )
    }
}

private const val WRITE_LIMIT = 20
private const val SETTLE_MILLIS = 4_000L

private val PROBE = InstalledApp(
    githubRepoId = 4_321L,
    packageName = "com.probe",
    slug = "probe",
    title = "Probe",
    iconUrl = null,
    versionTag = "v1.0.0",
)

private fun recordingFor(app: InstalledApp): InstallRecording = InstallRecording(
    app = app,
    versionCode = 1L,
    installedAt = Instant.ofEpochMilli(1_700_000_000_000),
)
