package app.yuki.core.installer

import app.yuki.core.model.DownloadSize
import app.yuki.core.model.SelfListing
import app.yuki.core.model.downloadSizeOf
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal val TEST_APK = File("/tmp/yuki-test.apk")

internal fun testRequest(
    githubRepoId: Long = 42L,
    versionTag: String = "v1.2.0",
): InstallRequest = InstallRequest(
    target = InstallTarget(githubRepoId, "acme-app", "Acme App", iconUrl = null),
    source = InstallSource("https://example.test/acme.apk", versionTag, "acme.apk"),
)

internal fun testSelfListing(githubRepoId: Long = 42L, releaseTag: String = "v1.2.0") =
    SelfListing(
        githubRepoId = githubRepoId,
        slug = "acme-app",
        packageName = "com.acme.app",
        title = "Acme App",
        iconUrl = "https://example.test/icon.png",
        releaseTag = releaseTag,
        versionCode = 12L,
    )

internal val NO_SELF_RELEASE = testSelfListing(releaseTag = "")

internal val TEST_TOTAL_BYTES = 1_000L

internal fun testSize(bytesDownloaded: Long, bytesTotal: Long = TEST_TOTAL_BYTES): DownloadSize =
    downloadSizeOf(bytesDownloaded = bytesDownloaded, bytesTotal = bytesTotal)

internal class FakeApkDownloader(
    private val sizes: List<DownloadSize> = listOf(testSize(500L)),
    private val failure: Throwable? = null,
    private val cancelMidway: Boolean = false,
) : ApkDownloader {
    val discarded: MutableList<File> = mutableListOf()

    var downloads: Int = 0
        private set

    override fun download(source: InstallSource): Flow<DownloadProgress> = flow {
        downloads += 1
        failure?.let { error -> throw error }
        if (cancelMidway) throw CancellationException("cancelled while downloading")
        sizes.forEach { size -> emit(DownloadProgress.Running(size)) }
        emit(DownloadProgress.Completed(TEST_APK))
    }

    override fun discard(apk: File) {
        discarded += apk
    }
}

internal class FakeApkIdentityReader(private val identity: ApkIdentity) : ApkIdentityReader {
    override fun read(apk: File): ApkIdentity = identity
}

internal class FakeInstallStrategy(private val outcomes: List<InstallOutcome>) : InstallStrategy {
    var installCount: Int = 0
        private set

    override fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome> = flow {
        installCount += 1
        outcomes.forEach { outcome -> emit(outcome) }
    }
}

internal class FakeInstallRecorder(private val recorded: MutableMap<Long, String>) :
    InstallRecorder {
    val records: MutableList<InstallRecord> = mutableListOf()

    override suspend fun recordedPackageName(githubRepoId: Long): String? = recorded[githubRepoId]

    override suspend fun record(record: InstallRecord) {
        records += record
        recorded[record.target.githubRepoId] = record.identity.packageName
    }
}

internal class FakePrivilegedInstaller(
    private val isReady: Boolean,
    private val strategy: InstallStrategy,
    private val outcome: InstallOutcome = InstallOutcome.Succeeded,
) : PrivilegedInstaller {
    val uninstalled: MutableList<String> = mutableListOf()

    override suspend fun isReady(): Boolean = isReady

    override fun strategy(): InstallStrategy = strategy

    override suspend fun uninstall(packageName: String): InstallOutcome {
        uninstalled += packageName

        return outcome
    }
}
