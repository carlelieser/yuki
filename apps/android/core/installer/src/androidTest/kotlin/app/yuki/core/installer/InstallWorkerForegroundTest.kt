package app.yuki.core.installer

import android.app.Notification
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import app.yuki.core.model.SelfListing
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstallWorkerForegroundTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun anExpeditedInstallCanRunInTheForeground() = runTest {
        val info = worker().getForegroundInfo()

        val title = info.notification.extras.getString(Notification.EXTRA_TITLE)
        assertEquals(context.getString(R.string.installer_notification_installing, TITLE), title)
        assertEquals(INSTALL_CHANNEL_ID, info.notification.channelId)
    }

    @Test
    fun theInstallChannelExistsOnceTheWorkerHasAskedForIt() = runTest {
        worker().getForegroundInfo()

        val manager = NotificationManagerCompat.from(context)

        assertNotNull(manager.getNotificationChannel(INSTALL_CHANNEL_ID))
    }

    private fun worker(): InstallWorker = TestListenableWorkerBuilder<InstallWorker>(context)
        .setInputData(REQUEST.toWorkData())
        .setWorkerFactory(IdleRunWorkerFactory())
        .build()
}

private const val TITLE = "DPI Zoom"

private val REQUEST = InstallRequest(
    target = InstallTarget(githubRepoId = 7L, slug = "dpi-zoom", title = TITLE, iconUrl = null),
    source = InstallSource("https://yuki.test/download", versionTag = "v1", assetName = null),
)

private class IdleRunWorkerFactory : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): ListenableWorker = InstallWorker(appContext, workerParameters, idleRun())
}

private fun idleRun(): InstallRun = InstallRun(
    coordinator = InstallCoordinator(
        downloader = IdleDownloader,
        recorder = IdleRecorder,
        strategies = InstallStrategySelector(
            identityReader = IdleIdentityReader,
            fallback = IdleStrategy,
            privileged = null,
        ),
    ),
    progress = IdleProgressStore,
    self = NO_SELF_RELEASE,
)

private val NO_SELF_RELEASE = SelfListing(
    githubRepoId = 0L,
    slug = "",
    packageName = "",
    title = "",
    iconUrl = "",
    releaseTag = "",
    versionCode = 0L,
)

private object IdleDownloader : ApkDownloader {
    override fun download(source: InstallSource): Flow<DownloadProgress> = emptyFlow()

    override fun discard(apk: File) = Unit
}

private object IdleRecorder : InstallRecorder {
    override suspend fun recordedPackageName(githubRepoId: Long): String? = null

    override suspend fun record(record: InstallRecord) = Unit
}

private object IdleIdentityReader : ApkIdentityReader {
    override fun read(apk: File): ApkIdentity = ApkIdentity("app.test", versionCode = 1L)
}

private object IdleStrategy : InstallStrategy {
    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = emptyFlow()
}

private object IdleProgressStore : InstallProgressStore {
    override fun observe(githubRepoId: Long): Flow<InstallProgress?> = emptyFlow()

    override fun observeActive(): Flow<List<InstallProgress>> = emptyFlow()

    override suspend fun find(githubRepoId: Long): InstallProgress? = null

    override suspend fun unsettled(): List<InstallProgress> = emptyList()

    override suspend fun write(progress: InstallProgress) = Unit

    override suspend fun clear(githubRepoId: Long) = Unit

    override suspend fun clearSettled() = Unit
}
