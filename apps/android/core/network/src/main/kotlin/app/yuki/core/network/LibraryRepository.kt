package app.yuki.core.network

import app.yuki.core.model.LibraryEntry
import javax.inject.Inject
import javax.inject.Singleton

interface LibraryRepository {
    suspend fun library(): Result<List<LibraryEntry>>

    suspend fun record(slug: String, versionTag: String): Result<Unit>
}

@Singleton
internal class NetworkLibraryRepository @Inject constructor(
    private val remote: LibraryRemoteDataSource,
) : LibraryRepository {
    override suspend fun library(): Result<List<LibraryEntry>> =
        runRemote("Load the library") {
            remote.library().results.map(LibraryEntryDto::toDomain)
        }

    override suspend fun record(slug: String, versionTag: String): Result<Unit> =
        runRemote("Add slug=$slug to the library") {
            remote.record(slug, versionTag)
        }
}
