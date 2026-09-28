package app.yuki.core.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

enum class BrowserAuthPurpose { SignIn, Link }

interface PendingBrowserAuth {
    suspend fun begin(purpose: BrowserAuthPurpose): String

    suspend fun claim(state: String, purpose: BrowserAuthPurpose): Boolean
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BrowserAuthPreferences

private val Context.browserAuthPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "browser_auth",
)

private val StateKey = stringPreferencesKey("state")
private val PurposeKey = stringPreferencesKey("purpose")
private val StartedAtKey = longPreferencesKey("startedAt")

private const val STATE_BYTES = 32
private const val PENDING_LIFETIME_MILLIS = 10 * 60 * 1000L

@Singleton
internal class DataStorePendingBrowserAuth(
    private val preferences: DataStore<Preferences>,
    private val clock: () -> Long,
) : PendingBrowserAuth {
    @Inject
    constructor(
        @BrowserAuthPreferences preferences: DataStore<Preferences>,
    ) : this(preferences, System::currentTimeMillis)

    private val random = SecureRandom()

    override suspend fun begin(purpose: BrowserAuthPurpose): String {
        val state = newState()

        preferences.edit { stored ->
            stored[StateKey] = state
            stored[PurposeKey] = purpose.name
            stored[StartedAtKey] = clock()
        }

        return state
    }

    override suspend fun claim(state: String, purpose: BrowserAuthPurpose): Boolean {
        var isClaimed = false

        preferences.edit { stored ->
            val isMatch = stored[StateKey] == state && stored[PurposeKey] == purpose.name
            val startedAt = stored[StartedAtKey] ?: return@edit
            if (!isMatch) return@edit

            isClaimed = clock() - startedAt <= PENDING_LIFETIME_MILLIS
            stored.clear()
        }

        return isClaimed
    }

    private fun newState(): String {
        val bytes = ByteArray(STATE_BYTES).also(random::nextBytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal object PendingBrowserAuthModule {
    @Provides
    @Singleton
    @BrowserAuthPreferences
    fun preferences(@ApplicationContext context: Context): DataStore<Preferences> =
        context.browserAuthPreferences

    @Provides
    @Singleton
    fun pendingBrowserAuth(implementation: DataStorePendingBrowserAuth): PendingBrowserAuth =
        implementation
}
