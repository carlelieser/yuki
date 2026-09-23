package app.yuki.core.shizuku

import app.yuki.core.installer.PrivilegedInstaller
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.flowOf

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ShizukuModule {
    @Binds
    @Singleton
    abstract fun bindGateway(gateway: RealShizukuGateway): ShizukuGateway

    @Binds
    @Singleton
    abstract fun bindUserServiceBinder(binder: ShizukuUserServiceBinder): UserServiceBinder

    @Binds
    @Singleton
    abstract fun bindPrivilegedInstaller(
        installer: ShizukuPrivilegedInstaller,
    ): PrivilegedInstaller

    companion object {
        @Provides
        @Singleton
        fun provideInstallerPackagePreference(): InstallerPackagePreference =
            InstallerPackagePreference { flowOf(SHELL_INSTALLER_PACKAGE) }
    }
}
