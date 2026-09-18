package app.yuki.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionPreferences

private val Context.sessionPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "session",
)

private val TokenKey = stringPreferencesKey("token")
private val UserIdKey = stringPreferencesKey("userId")
private val NameKey = stringPreferencesKey("name")
private val EmailKey = stringPreferencesKey("email")
private val ImageUrlKey = stringPreferencesKey("imageUrl")

interface SessionStore {
    val session: Flow<AuthSession?>

    suspend fun read(): AuthSession?

    suspend fun store(session: AuthSession)

    suspend fun updateAccount(account: AuthAccount)

    suspend fun clear()
}

@Singleton
internal class DataStoreSessionStore @Inject constructor(
    @param:SessionPreferences private val preferences: DataStore<Preferences>,
) : SessionStore {
    override val session: Flow<AuthSession?> = preferences.data.map(::decode)

    override suspend fun read(): AuthSession? = session.first()

    override suspend fun store(session: AuthSession) {
        preferences.edit { stored -> stored.write(session) }
    }

    override suspend fun updateAccount(account: AuthAccount) {
        preferences.edit { stored -> stored.write(account) }
    }

    override suspend fun clear() {
        preferences.edit { stored -> stored.clear() }
    }
}

private fun MutablePreferences.write(session: AuthSession) {
    this[TokenKey] = session.token
    write(session.account)
}

private fun MutablePreferences.write(account: AuthAccount) {
    this[UserIdKey] = account.id
    this[NameKey] = account.name
    this[EmailKey] = account.email
    account.imageUrl?.let { url -> this[ImageUrlKey] = url } ?: remove(ImageUrlKey)
}

private fun decode(stored: Preferences): AuthSession? {
    val token = stored[TokenKey] ?: return null
    val id = stored[UserIdKey] ?: return null

    return AuthSession(
        token = token,
        account = AuthAccount(
            id = id,
            name = stored[NameKey].orEmpty(),
            email = stored[EmailKey].orEmpty(),
            imageUrl = stored[ImageUrlKey],
        ),
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal object SessionStoreModule {
    @Provides
    @Singleton
    @SessionPreferences
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        context.sessionPreferences

    @Provides
    @Singleton
    fun sessionStore(implementation: DataStoreSessionStore): SessionStore = implementation
}
