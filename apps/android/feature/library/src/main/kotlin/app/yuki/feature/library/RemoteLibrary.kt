package app.yuki.feature.library

import app.yuki.core.model.LibraryEntry

interface RemoteLibrary {
    suspend fun entries(): List<LibraryEntry>
}
