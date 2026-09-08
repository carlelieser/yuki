package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

@Singleton
class InstallCoordinator @Inject constructor(
    private val downloader: ApkDownloader,
    private val recorder: InstallRecorder,
    private val strategies: InstallStrategySelector,
) {
    fun install(request: InstallRequest): Flow<InstallState> = flow {
        val apk = downloadApk(request.source)
        val staged = StagedApk(apk, strategies.identityReader.read(apk))
        guardPackageIdentity(request.target.githubRepoId, staged.identity)
        runStrategy(request, staged)
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(InstallState.Failed(error.toInstallFailure()))
    }

    private suspend fun FlowCollector<InstallState>.downloadApk(source: InstallSource): File {
        var downloaded: File? = null

        downloader.download(source).collect { progress ->
            when (progress) {
                is DownloadProgress.Running -> emit(InstallState.Downloading(progress.fraction))
                is DownloadProgress.Completed -> downloaded = progress.apk
            }
        }

        return downloaded ?: throw InstallException(
            InstallFailure.DownloadFailed,
            "Download completed without an APK for url=${source.downloadUrl}",
        )
    }

    private suspend fun guardPackageIdentity(githubRepoId: Long, identity: ApkIdentity) {
        val recorded = recorder.recordedPackageName(githubRepoId) ?: return
        if (recorded == identity.packageName) return

        throw InstallException(
            InstallFailure.PackageMismatch,
            "Refusing install for githubRepoId=$githubRepoId: " +
                "recorded package $recorded but APK declares ${identity.packageName}",
        )
    }

    private suspend fun FlowCollector<InstallState>.runStrategy(
        request: InstallRequest,
        staged: StagedApk,
    ) {
        val strategy = strategies.select()

        strategy.install(staged.file, staged.identity).collect { outcome ->
            emit(outcome.toInstallState(request.source.versionTag))
            if (outcome is InstallOutcome.Succeeded) {
                recorder.record(request.toRecord(staged.identity))
            }
        }
    }
}

private fun InstallRequest.toRecord(identity: ApkIdentity): InstallRecord =
    InstallRecord(target, identity, source.versionTag)

private fun InstallOutcome.toInstallState(versionTag: String): InstallState = when (this) {
    is InstallOutcome.Succeeded -> InstallState.Installed(versionTag)
    is InstallOutcome.AwaitingUserAction -> InstallState.PendingUserAction
    is InstallOutcome.Failed -> InstallState.Failed(reason)
}

private fun Throwable.toInstallFailure(): InstallFailure = when (this) {
    is InstallException -> failure
    else -> InstallFailure.Rejected(message ?: this::class.java.name)
}
