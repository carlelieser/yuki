package app.yuki.core.installer

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class InstallerModule {
    @Binds
    abstract fun bindDownloader(downloader: DownloadManagerApkDownloader): ApkDownloader

    @Binds
    abstract fun bindIdentityReader(reader: PackageManagerApkIdentityReader): ApkIdentityReader

    @Binds
    abstract fun bindFallbackStrategy(strategy: SystemInstallStrategy): InstallStrategy

    @BindsOptionalOf
    abstract fun optionalPrivilegedInstaller(): PrivilegedInstaller
}

@Module
@InstallIn(SingletonComponent::class)
internal object InstallStrategySelectorModule {
    @Provides
    @Singleton
    fun provideSelector(
        identityReader: ApkIdentityReader,
        fallback: InstallStrategy,
        privileged: Optional<PrivilegedInstaller>,
    ): InstallStrategySelector =
        InstallStrategySelector(identityReader, fallback, privileged.orElse(null))
}
