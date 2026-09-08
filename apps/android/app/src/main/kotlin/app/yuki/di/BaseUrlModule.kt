package app.yuki.di

import app.yuki.BuildConfig
import app.yuki.core.network.YukiBaseUrl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BaseUrlModule {
    @Provides
    @Singleton
    @YukiBaseUrl
    fun baseUrl(): String = BuildConfig.YUKI_BASE_URL
}
