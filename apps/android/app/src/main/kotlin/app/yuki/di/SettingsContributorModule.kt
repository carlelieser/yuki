package app.yuki.di

import app.yuki.core.settings.api.SettingsContributor
import app.yuki.feature.account.AccountSettingsContributor
import app.yuki.feature.library.LibrarySettingsContributor
import app.yuki.feature.settings.SystemSettingsContributor
import app.yuki.settings.LegalSettingsContributor
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsContributorModule {
    @Binds
    @IntoSet
    fun accountContributor(implementation: AccountSettingsContributor): SettingsContributor

    @Binds
    @IntoSet
    fun libraryContributor(implementation: LibrarySettingsContributor): SettingsContributor

    @Binds
    @IntoSet
    fun systemContributor(implementation: SystemSettingsContributor): SettingsContributor

    @Binds
    @IntoSet
    fun legalContributor(implementation: LegalSettingsContributor): SettingsContributor
}
