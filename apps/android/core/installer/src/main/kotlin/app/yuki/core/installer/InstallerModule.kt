package app.yuki.core.installer

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Duration
import java.util.Optional
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
internal abstract class InstallerModule {
    @Binds
    abstract fun bindDownloader(downloader: HttpApkDownloader): ApkDownloader

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

@Module
@InstallIn(SingletonComponent::class)
internal object ApkDownloadClientModule {
    @Provides
    @Singleton
    @ApkDownloadClient
    fun provideClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT)
        .readTimeout(READ_TIMEOUT)
        .build()
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class ApkDownloadClient

private val CONNECT_TIMEOUT: Duration = Duration.ofSeconds(30)
private val READ_TIMEOUT: Duration = Duration.ofSeconds(60)
