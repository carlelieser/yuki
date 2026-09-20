package app.yuki.di

import app.yuki.feature.account.AccountSettingsNavigation
import app.yuki.feature.library.LibrarySettingsNavigation
import app.yuki.settings.SettingsNavigationHolder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsNavigationModule {
    @Binds
    @Singleton
    fun accountSettingsNavigation(
        implementation: SettingsNavigationHolder,
    ): AccountSettingsNavigation

    @Binds
    @Singleton
    fun librarySettingsNavigation(
        implementation: SettingsNavigationHolder,
    ): LibrarySettingsNavigation
}
