package app.yuki.di

import app.yuki.core.installer.InstallRecorder
import app.yuki.core.installer.InstalledListings
import app.yuki.feature.listing.ListingInstallGateway
import app.yuki.install.CoordinatorListingInstallGateway
import app.yuki.install.RepositoryRemoteLibrary
import app.yuki.install.StoreInstalledListings
import app.yuki.install.SyncingInstallRecorder
import app.yuki.feature.library.RemoteLibrary
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

    @Binds
    @Singleton
    fun installedListings(implementation: StoreInstalledListings): InstalledListings

    @Binds
    @Singleton
    fun installRecorder(implementation: SyncingInstallRecorder): InstallRecorder

    @Binds
    @Singleton
    fun remoteLibrary(implementation: RepositoryRemoteLibrary): RemoteLibrary
}
