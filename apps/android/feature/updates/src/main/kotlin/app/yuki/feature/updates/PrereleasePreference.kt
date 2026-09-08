package app.yuki.feature.updates

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun interface PrereleasePreference {
    fun includePrereleases(): Flow<Boolean>
}

internal const val INCLUDE_PRERELEASES_DEFAULT = false

@Module
@InstallIn(SingletonComponent::class)
internal object PrereleasePreferenceModule {
    @Provides
    @Singleton
    fun providePreference(): PrereleasePreference =
        PrereleasePreference { flowOf(INCLUDE_PRERELEASES_DEFAULT) }
}
