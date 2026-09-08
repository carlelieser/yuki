package app.yuki.feature.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SettingsPreferences

const val SETTINGS_PREFERENCES_FILE = "settings"

private val Context.settingsPreferences: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_PREFERENCES_FILE,
)

@Module
@InstallIn(SingletonComponent::class)
internal object SettingsPreferencesModule {
    @Provides
    @Singleton
    @SettingsPreferences
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsPreferences
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class PreferenceStoreModule {
    @Binds
    @Singleton
    abstract fun bindStore(store: DataStorePreferenceStore): PreferenceStore
}
