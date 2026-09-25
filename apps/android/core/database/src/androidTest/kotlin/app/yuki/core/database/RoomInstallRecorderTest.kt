package app.yuki.core.database

import android.database.sqlite.SQLiteFullException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.installer.ApkIdentity
import app.yuki.core.installer.InstallException
import app.yuki.core.installer.InstallRecord
import app.yuki.core.installer.InstallRecorder
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstalledApp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInstallRecorderTest {
    private lateinit var database: YukiDatabase
    private lateinit var store: InstallStore
    private lateinit var recorder: InstallRecorder

    @Before
    fun createRecorder() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, YukiDatabase::class.java).build()
        store = RoomInstallStore(database.installDao())
        recorder = RoomInstallRecorder(store, Clock.fixed(RECORDED_AT, ZoneOffset.UTC))
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aRecordTheDatabaseCannotStoreIsReportedAsNotRecorded() = runTest {
        val clock = Clock.fixed(RECORDED_AT, ZoneOffset.UTC)
        val full = RoomInstallRecorder(FullInstallStore(store), clock)

        val error = runCatching {
            full.record(recordOf(packageName = "com.termux", versionTag = "v0.118.0"))
        }.exceptionOrNull()

        assertEquals(InstallFailure.NotRecorded, (error as InstallException).failure)
    }

    @Test
    fun aFirstInstallHasNoRecordedPackageName() = runTest {
        assertNull(recorder.recordedPackageName(TARGET.githubRepoId))
    }

    @Test
    fun recordingAnInstallMakesItReadableAsAnInstalledApp() = runTest {
        recorder.record(recordOf(packageName = "com.termux", versionTag = "v0.118.0"))

        val expected = InstalledApp(
            githubRepoId = TARGET.githubRepoId,
            packageName = "com.termux",
            slug = TARGET.slug,
            title = TARGET.title,
            iconUrl = TARGET.iconUrl,
            versionTag = "v0.118.0",
        )
        assertEquals(listOf(expected), store.installs())
    }

    @Test
    fun recordedPackageNameSurvivesForTheMismatchGuard() = runTest {
        recorder.record(recordOf(packageName = "com.termux", versionTag = "v0.118.0"))

        assertEquals("com.termux", recorder.recordedPackageName(TARGET.githubRepoId))
        assertNull(recorder.recordedPackageName(OTHER_REPO_ID))
    }

    @Test
    fun anUpdateReplacesTheRowForTheSameRepoId() = runTest {
        recorder.record(recordOf(packageName = "com.termux", versionTag = "v0.118.0"))
        recorder.record(recordOf(packageName = "com.termux", versionTag = "v0.119.0"))

        assertEquals(1, store.installs().size)
        assertEquals("v0.119.0", store.installs().single().versionTag)
    }
}

private val RECORDED_AT: Instant = Instant.ofEpochMilli(1_700_000_000_000)
private const val OTHER_REPO_ID = 9_999L

private val TARGET = InstallTarget(
    githubRepoId = 1_234L,
    slug = "termux",
    title = "Termux",
    iconUrl = "https://yuki.app/termux.png",
)

private fun recordOf(packageName: String, versionTag: String): InstallRecord = InstallRecord(
    target = TARGET,
    identity = ApkIdentity(packageName = packageName, versionCode = 118L),
    versionTag = versionTag,
)

private class FullInstallStore(private val delegate: InstallStore) : InstallStore by delegate {
    override suspend fun record(recording: InstallRecording) =
        throw SQLiteFullException("disk full")
}
