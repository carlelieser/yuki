package app.yuki.di

import app.yuki.feature.listing.ListingInstallGateway
import app.yuki.install.CoordinatorListingInstallGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface InstallGatewayModule {
    @Binds
    @Singleton
    fun listingInstallGateway(
        implementation: CoordinatorListingInstallGateway,
    ): ListingInstallGateway
}
