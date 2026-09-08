package app.yuki.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
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
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val RECENT_SEARCH_LIMIT = 8

private const val ENTRY_SEPARATOR = "\n"

private val Context.recentSearchPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "recent_searches",
)

private val RecentSearchesKey = stringPreferencesKey("queries")

interface RecentSearchStore {
    val recentSearches: Flow<List<String>>

    suspend fun remember(query: String)

    suspend fun forget(query: String)

    suspend fun clear()
}

internal fun promote(existing: List<String>, query: String): List<String> =
    (listOf(query) + existing.filterNot { entry -> entry.equals(query, ignoreCase = true) })
        .take(RECENT_SEARCH_LIMIT)

@Singleton
internal class PreferenceRecentSearchStore @Inject constructor(
    private val preferences: DataStore<Preferences>,
) : RecentSearchStore {
    override val recentSearches: Flow<List<String>> =
        preferences.data.map { stored -> decode(stored[RecentSearchesKey]) }

    override suspend fun remember(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return

        preferences.edit { stored ->
            stored[RecentSearchesKey] = encode(promote(decode(stored[RecentSearchesKey]), trimmed))
        }
    }

    override suspend fun forget(query: String) {
        preferences.edit { stored ->
            val remaining = decode(stored[RecentSearchesKey]).filterNot { entry -> entry == query }
            stored[RecentSearchesKey] = encode(remaining)
        }
    }

    override suspend fun clear() {
        preferences.edit { stored -> stored.remove(RecentSearchesKey) }
    }
}

private fun decode(raw: String?): List<String> =
    raw?.split(ENTRY_SEPARATOR)?.filter { entry -> entry.isNotEmpty() }.orEmpty()

private fun encode(entries: List<String>): String = entries.joinToString(ENTRY_SEPARATOR)

@Module
@InstallIn(SingletonComponent::class)
internal object RecentSearchModule {
    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext context: Context): DataStore<Preferences> =
        context.recentSearchPreferences

    @Provides
    @Singleton
    fun provideStore(store: PreferenceRecentSearchStore): RecentSearchStore = store
}
