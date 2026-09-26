package app.yuki.di

import app.yuki.core.installer.InstallModePreference
import app.yuki.core.installer.PreferredInstaller
import app.yuki.core.shizuku.InstallerPackagePreference
import app.yuki.feature.settings.InstallMode
import app.yuki.feature.settings.YukiPreferenceReader
import app.yuki.feature.updates.AutoUpdateCheckPreference
import app.yuki.feature.updates.PrereleasePreference
import app.yuki.feature.updates.UpdateNotificationPreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.flow.map

@Module
@InstallIn(SingletonComponent::class)
internal object PreferencesModule {
    @Provides
    @Singleton
    fun prereleasePreference(reader: YukiPreferenceReader): PrereleasePreference =
        PrereleasePreference { reader.includePrereleases() }

    @Provides
    @Singleton
    fun autoUpdateCheckPreference(reader: YukiPreferenceReader): AutoUpdateCheckPreference =
        AutoUpdateCheckPreference { reader.isAutoUpdateCheckEnabled() }

    @Provides
    @Singleton
    fun updateNotificationPreference(reader: YukiPreferenceReader): UpdateNotificationPreference =
        UpdateNotificationPreference { reader.isUpdateNotificationEnabled() }

    @Provides
    @Singleton
    fun installerPackagePreference(
        reader: YukiPreferenceReader,
    ): InstallerPackagePreference = InstallerPackagePreference { reader.installerPackage() }

    @Provides
    @Singleton
    fun installModePreference(reader: YukiPreferenceReader): InstallModePreference =
        InstallModePreference { reader.installMode().map(::toPreferredInstaller) }
}

private fun toPreferredInstaller(mode: InstallMode): PreferredInstaller = when (mode) {
    InstallMode.Shizuku -> PreferredInstaller.Privileged
    InstallMode.System -> PreferredInstaller.System
}
