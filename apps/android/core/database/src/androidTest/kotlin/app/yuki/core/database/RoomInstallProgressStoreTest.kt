package app.yuki.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomInstallProgressStoreTest {
    private lateinit var database: YukiDatabase
    private lateinit var clock: MutableClock
    private lateinit var store: InstallProgressStore

    @Before
    fun createStore() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, YukiDatabase::class.java).build()
        clock = MutableClock(START_MILLIS)
        store = RoomInstallProgressStore(database.installProgressDao(), clock)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun repeatedWritesKeepTheOriginalCreatedAt() = runTest {
        store.write(progressFor(AURORA, bytesDownloaded = 100L))
        clock.advanceBy(1_000L)
        store.write(progressFor(AURORA, bytesDownloaded = 500L))
        clock.advanceBy(1_000L)
        store.write(progressFor(AURORA, bytesDownloaded = 900L))

        val row = rowFor(AURORA.githubRepoId)

        assertEquals(START_MILLIS, row.createdAt)
        assertEquals(START_MILLIS + 2_000L, row.updatedAt)
    }

    @Test
    fun aRowRewrittenAfterBeingClearedGetsAFreshCreatedAt() = runTest {
        store.write(progressFor(AURORA, bytesDownloaded = 100L))
        store.clear(AURORA.githubRepoId)
        clock.advanceBy(5_000L)
        store.write(progressFor(AURORA, bytesDownloaded = 100L))

        assertEquals(START_MILLIS + 5_000L, rowFor(AURORA.githubRepoId).createdAt)
    }

    @Test
    fun activeProgressIsOrderedByWhenEachInstallStarted() = runTest {
        store.write(progressFor(AURORA, bytesDownloaded = 100L))
        clock.advanceBy(1_000L)
        store.write(progressFor(TERMUX, bytesDownloaded = 100L))
        clock.advanceBy(1_000L)
        store.write(progressFor(AURORA, bytesDownloaded = 900L))

        val ordered = store.observeActive().first().map(InstallProgress::githubRepoId)

        assertEquals(listOf(AURORA.githubRepoId, TERMUX.githubRepoId), ordered)
    }

    @Test
    fun anInstallInProgressIsReportedAsUnsettled() = runTest {
        store.write(
            InstallProgress(AURORA, VERSION_TAG, InstallState.Installing),
        )

        assertEquals(
            listOf(AURORA.githubRepoId),
            store.unsettled().map(InstallProgress::githubRepoId),
        )
    }

    @Test
    fun aSettledRowIsClearedEvenWhenItsInstallRecordIsGone() = runTest {
        store.write(InstallProgress(AURORA, VERSION_TAG, InstallState.Installed(VERSION_TAG)))

        store.clearSettled()

        assertEquals(
            emptyList<InstallProgressEntity>(),
            database.installProgressDao().observeAll().first(),
        )
    }

    @Test
    fun clearingSettledRowsKeepsAnInstallThatIsStillRunning() = runTest {
        store.write(InstallProgress(AURORA, VERSION_TAG, InstallState.Installed(VERSION_TAG)))
        store.write(progressFor(TERMUX, bytesDownloaded = 100L))

        store.clearSettled()

        assertEquals(
            listOf(TERMUX.githubRepoId),
            store.observeActive().first().map(InstallProgress::githubRepoId),
        )
    }

    private suspend fun rowFor(githubRepoId: Long): InstallProgressEntity =
        database.installProgressDao().observeAll().first()
            .single { entity -> entity.githubRepoId == githubRepoId }
}

private class MutableClock(private var millis: Long) : Clock() {
    fun advanceBy(amount: Long) {
        millis += amount
    }

    override fun getZone(): ZoneOffset = ZoneOffset.UTC

    override fun withZone(zone: java.time.ZoneId): Clock = this

    override fun instant(): Instant = Instant.ofEpochMilli(millis)

    override fun millis(): Long = millis
}

private const val START_MILLIS = 1_700_000_000_000L
private const val VERSION_TAG = "v1.0.0"

private val AURORA = InstallTarget(
    githubRepoId = 5_678L,
    slug = "aurora-store",
    title = "Aurora Store",
    iconUrl = null,
)

private val TERMUX = InstallTarget(
    githubRepoId = 1_234L,
    slug = "termux",
    title = "Termux",
    iconUrl = null,
)

private fun progressFor(target: InstallTarget, bytesDownloaded: Long): InstallProgress =
    InstallProgress(
        target = target,
        versionTag = VERSION_TAG,
        state = InstallState.Downloading(
            downloadSizeOf(bytesDownloaded = bytesDownloaded, bytesTotal = 1_000L),
        ),
    )
