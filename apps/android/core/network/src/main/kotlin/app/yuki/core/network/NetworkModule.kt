package app.yuki.core.network

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YukiBaseUrl

@Module
@InstallIn(SingletonComponent::class)
internal object NetworkProviders {
    @Provides
    @Singleton
    fun httpClient(@YukiBaseUrl baseUrl: String): HttpClient = YukiHttpClient.create(baseUrl)
}

@Module
@InstallIn(SingletonComponent::class)
internal interface NetworkBindings {
    @Binds
    @Singleton
    fun listingRepository(implementation: NetworkListingRepository): ListingRepository
}
