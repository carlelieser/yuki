package app.yuki.feature.library

import app.yuki.core.database.InstallStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.LibraryEntry
import javax.inject.Inject

data class LibrarySyncResult(
    val uploaded: Int = 0,
    val failed: Int = 0,
)

internal class LibrarySync @Inject constructor(
    private val detection: DetectedInstallRefresh,
    private val store: InstallStore,
    private val remote: RemoteLibrary,
) {
    suspend fun run(): LibrarySyncResult {
        detection.reconcile()

        val published = remote.entries().map(::libraryKey).toSet()
        val missing = store.installs().filterNot { app -> libraryKey(app) in published }

        return missing.fold(LibrarySyncResult()) { result, app -> result.plus(upload(app)) }
    }

    private suspend fun upload(app: InstalledApp): LibrarySyncResult =
        remote.record(app.slug, app.versionTag).fold(
            onSuccess = { LibrarySyncResult(uploaded = 1) },
            onFailure = { LibrarySyncResult(failed = 1) },
        )
}

private fun LibrarySyncResult.plus(other: LibrarySyncResult) = LibrarySyncResult(
    uploaded = uploaded + other.uploaded,
    failed = failed + other.failed,
)

private fun libraryKey(app: InstalledApp): Long = app.githubRepoId

private fun libraryKey(entry: LibraryEntry): Long = entry.githubRepoId
