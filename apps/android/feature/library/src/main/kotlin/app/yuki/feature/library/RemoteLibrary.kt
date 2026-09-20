package app.yuki.feature.library

import app.yuki.core.model.LibraryEntry

interface RemoteLibrary {
    suspend fun entries(): List<LibraryEntry>

    suspend fun record(slug: String, versionTag: String): Result<Unit>
}
