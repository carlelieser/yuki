package app.yuki.feature.library

import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.AuthAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeSessionStore(initial: AuthSession? = null) : SessionStore {
    private val stored = MutableStateFlow(initial)

    override val session: Flow<AuthSession?> = stored.asStateFlow()

    override suspend fun read(): AuthSession? = stored.value

    override suspend fun store(session: AuthSession) {
        stored.value = session
    }

    override suspend fun updateAccount(account: AuthAccount) {
        stored.value = stored.value?.copy(account = account)
    }

    override suspend fun clear() {
        stored.value = null
    }
}
