package app.yuki.core.auth

import app.yuki.core.network.AuthTokenSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
internal class StoredAuthTokenSource @Inject constructor(
    store: SessionStore,
) : AuthTokenSource {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val token = store.session
        .map { session -> StoredToken(session?.token) }
        .stateIn(scope, SharingStarted.Eagerly, initialValue = null)

    override suspend fun token(): String? = (token.value ?: token.filterNotNull().first()).value
}

private data class StoredToken(val value: String?)

@Module
@InstallIn(SingletonComponent::class)
internal interface AuthTokenBindings {
    @Binds
    @Singleton
    fun authTokenSource(implementation: StoredAuthTokenSource): AuthTokenSource
}
