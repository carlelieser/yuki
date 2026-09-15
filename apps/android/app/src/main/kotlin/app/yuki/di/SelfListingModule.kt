package app.yuki.di

import app.yuki.BuildConfig
import app.yuki.core.model.SelfListing
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object SelfListingModule {
    @Provides
    @Singleton
    fun selfListing(): SelfListing = SelfListing(
        githubRepoId = BuildConfig.YUKI_LISTING_REPO_ID,
        slug = BuildConfig.YUKI_LISTING_SLUG,
        packageName = BuildConfig.APPLICATION_ID,
        title = BuildConfig.YUKI_LISTING_TITLE,
        iconUrl = BuildConfig.YUKI_LISTING_ICON_URL,
        releaseTag = BuildConfig.YUKI_RELEASE_TAG,
        versionCode = BuildConfig.VERSION_CODE.toLong(),
    )
}
