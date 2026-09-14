package app.yuki.core.installer

import kotlinx.coroutines.flow.Flow

interface InstalledListings {
    fun observeInstalledIds(): Flow<Set<Long>>
}
