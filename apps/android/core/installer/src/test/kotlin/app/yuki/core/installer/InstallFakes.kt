package app.yuki.core.installer

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

internal class FakeApkDownloader(
    private val fractions: List<Float> = listOf(0.5f),
    private val failure: InstallException? = null,
    private val cancelMidway: Boolean = false,
) : ApkDownloader {
    override fun download(source: InstallSource): Flow<DownloadProgress> = flow {
        failure?.let { error -> throw error }
        if (cancelMidway) throw CancellationException("cancelled while downloading")
        fractions.forEach { fraction -> emit(DownloadProgress.Running(fraction)) }
        emit(DownloadProgress.Completed(TEST_APK))
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
) : PrivilegedInstaller {
    override suspend fun isReady(): Boolean = isReady

    override fun strategy(): InstallStrategy = strategy
}
