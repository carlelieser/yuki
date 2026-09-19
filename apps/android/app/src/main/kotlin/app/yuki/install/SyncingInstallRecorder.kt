package app.yuki.install

import app.yuki.core.installer.InstallRecord
import app.yuki.core.installer.InstallRecorder
import app.yuki.core.installer.LocalInstallRecorder
import app.yuki.core.network.LibraryRepository
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LibrarySync"

internal fun interface SyncFailureLogger {
    fun onFailure(message: String, error: Throwable)
}

@Singleton
internal class SyncingInstallRecorder(
    private val local: InstallRecorder,
    private val library: LibraryRepository,
    private val logFailure: SyncFailureLogger,
) : InstallRecorder {
    @Inject
    constructor(
        @LocalInstallRecorder local: InstallRecorder,
        library: LibraryRepository,
    ) : this(
        local = local,
        library = library,
        logFailure = { message, error -> android.util.Log.w(TAG, message, error) },
    )

    override suspend fun recordedPackageName(githubRepoId: Long): String? =
        local.recordedPackageName(githubRepoId)

    override suspend fun record(record: InstallRecord) {
        local.record(record)
        publish(record)
    }

    private suspend fun publish(record: InstallRecord) {
        library.record(record.target.slug, record.versionTag).onFailure { error ->
            logFailure.onFailure("Could not add ${record.target.slug} to the library", error)
        }
    }
}
