package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpApkDownloaderTest {
    private val server = MockWebServer()
    private val directory: File = Files.createTempDirectory("yuki-apks").toFile()
    private val client = OkHttpClient.Builder().readTimeout(300, TimeUnit.MILLISECONDS).build()
    private val downloader = HttpApkDownloader(client, directory)

    @Before
    fun startServer() {
        server.start()
    }

    @After
    fun stopServer() {
        server.close()
        directory.deleteRecursively()
    }

    @Test
    fun `a download ends with the apk saved in full`() = runBlocking {
        val apk = bytes(700 * 1024)
        server.enqueue(MockResponse.Builder().body(Buffer().write(apk)).build())

        val progress = downloader.download(source()).toList()

        val saved = (progress.last() as DownloadProgress.Completed).apk
        assertArrayEquals(apk, saved.readBytes())
        assertEquals(directory, saved.parentFile)
    }

    @Test
    fun `progress climbs towards the advertised size`() = runBlocking {
        server.enqueue(MockResponse.Builder().body(Buffer().write(bytes(700 * 1024))).build())

        val sizes = downloader.download(source()).toList()
            .filterIsInstance<DownloadProgress.Running>()
            .map { running -> running.size }

        assertEquals(0L, sizes.first().bytesDownloaded)
        assertEquals(700L * 1024, sizes.last().bytesDownloaded)
        assertEquals(700L * 1024, sizes.last().bytesTotal)
        assertEquals(sizes.map { it.bytesDownloaded }.sorted(), sizes.map { it.bytesDownloaded })
    }

    @Test
    fun `a redirect to the release asset is followed`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(302).addHeader("Location", server.url("/asset.apk")).build(),
        )
        server.enqueue(MockResponse.Builder().body(Buffer().write(bytes(10))).build())

        val progress = downloader.download(source()).toList()

        assertTrue(progress.last() is DownloadProgress.Completed)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `a missing release fails with its http status`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(404).build())

        assertEquals(InstallFailure.DownloadFailed(httpStatus = 404), failureOf(source()))
    }

    @Test
    fun `a download that stops sending data fails without a status`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .body(Buffer().write(bytes(1024)))
                .bodyDelay(2, TimeUnit.SECONDS)
                .build(),
        )

        assertEquals(InstallFailure.DownloadFailed(httpStatus = null), failureOf(source()))
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `an empty download is unreadable`() = runBlocking {
        server.enqueue(MockResponse.Builder().body("").build())

        assertEquals(InstallFailure.DownloadUnreadable, failureOf(source()))
    }

    @Test
    fun `cancelling while the server is silent stops at once and leaves nothing behind`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .body(Buffer().write(bytes(1024)))
                .bodyDelay(10, TimeUnit.SECONDS)
                .build(),
        )
        val patient = HttpApkDownloader(OkHttpClient(), directory)

        val started = CompletableDeferred<Unit>()
        val collecting = launch(Dispatchers.IO) {
            patient.download(source()).collect { started.complete(Unit) }
        }
        started.await()

        val elapsed = measureTimeMillis {
            collecting.cancel()
            collecting.join()
        }

        assertTrue(elapsed < CANCEL_BUDGET_MILLIS)
        assertTrue(directory.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun `discarding removes the saved apk`() = runBlocking {
        server.enqueue(MockResponse.Builder().body(Buffer().write(bytes(10))).build())
        val saved = (downloader.download(source()).toList().last() as DownloadProgress.Completed).apk

        downloader.discard(saved)

        assertFalse(saved.exists())
    }

    private suspend fun failureOf(source: InstallSource): InstallFailure =
        (runCatching { downloader.download(source).toList() }.exceptionOrNull() as InstallException)
            .failure

    private fun source() = InstallSource(
        downloadUrl = server.url("/download").toString(),
        versionTag = "v1.0.0",
        assetName = "app.apk",
    )
}

private const val CANCEL_BUDGET_MILLIS = 2_000L

private fun bytes(count: Int): ByteArray = ByteArray(count) { index -> (index % 251).toByte() }
