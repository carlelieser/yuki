package app.yuki.core.installer

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Optional
import javax.inject.Qualifier
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

    @Binds
    abstract fun bindWorkLiveness(
        liveness: WorkManagerInstallWorkLiveness,
    ): InstallWorkLiveness

    @BindsOptionalOf
    abstract fun optionalPrivilegedInstaller(): PrivilegedInstaller
}

@Module
@InstallIn(SingletonComponent::class)
internal object InstallStrategySelectorModule {
    @Provides
    @Singleton
    @Gated
    fun provideGatedInstaller(
        privileged: Optional<PrivilegedInstaller>,
        preference: InstallModePreference,
    ): Optional<PrivilegedInstaller> =
        privileged.map { installer -> InstallModeGatedInstaller(installer, preference) }

    @Provides
    @Singleton
    fun provideSelector(
        identityReader: ApkIdentityReader,
        fallback: InstallStrategy,
        @Gated privileged: Optional<PrivilegedInstaller>,
    ): InstallStrategySelector =
        InstallStrategySelector(identityReader, fallback, privileged.orElse(null))

    @Provides
    @Singleton
    fun provideUninstaller(@Gated privileged: Optional<PrivilegedInstaller>): Uninstaller =
        Uninstaller(privileged.orElse(null))
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class Gated
