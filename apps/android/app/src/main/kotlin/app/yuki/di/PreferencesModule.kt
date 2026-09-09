package app.yuki.di

import app.yuki.feature.settings.YukiPreferenceReader
import app.yuki.feature.updates.PrereleasePreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object PreferencesModule {
    @Provides
    @Singleton
    fun prereleasePreference(reader: YukiPreferenceReader): PrereleasePreference =
        PrereleasePreference { reader.includePrereleases() }
}
