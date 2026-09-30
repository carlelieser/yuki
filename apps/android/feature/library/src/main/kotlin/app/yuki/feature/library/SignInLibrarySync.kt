package app.yuki.feature.library

import app.yuki.core.auth.SessionStore
import javax.inject.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

class SignInLibrarySync @Inject internal constructor(
    private val sessions: SessionStore,
    private val sync: LibrarySync,
) {
    suspend fun follow() {
        sessions.session
            .map { session -> session?.account?.id }
            .distinctUntilChanged()
            .drop(1)
            .filterNotNull()
            .collect { sync.run() }
    }
}
