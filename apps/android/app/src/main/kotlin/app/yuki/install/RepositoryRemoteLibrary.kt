package app.yuki.install

import app.yuki.core.model.LibraryEntry
import app.yuki.core.network.LibraryRepository
import app.yuki.feature.library.RemoteLibrary
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RepositoryRemoteLibrary @Inject constructor(
    private val repository: LibraryRepository,
) : RemoteLibrary {
    override suspend fun entries(): List<LibraryEntry> =
        repository.library().getOrDefault(emptyList())

    override suspend fun record(slug: String, versionTag: String): Result<Unit> =
        repository.record(slug, versionTag)
}
