package app.yuki.core.network

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import java.util.Optional
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
    fun httpClient(
        @YukiBaseUrl baseUrl: String,
        tokens: Optional<AuthTokenSource>,
    ): HttpClient = YukiHttpClient.create(
        baseUrl = baseUrl,
        tokens = tokens.orElse(AuthTokenSource { null }),
    )
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class NetworkBindings {
    @BindsOptionalOf
    abstract fun optionalAuthTokenSource(): AuthTokenSource

    @Binds
    @Singleton
    abstract fun listingRepository(implementation: NetworkListingRepository): ListingRepository

    @Binds
    @Singleton
    abstract fun authRepository(implementation: NetworkAuthRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun libraryRepository(implementation: NetworkLibraryRepository): LibraryRepository
}
