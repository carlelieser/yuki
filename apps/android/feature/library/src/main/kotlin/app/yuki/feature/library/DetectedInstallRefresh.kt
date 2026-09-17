package app.yuki.feature.library

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

interface DetectedInstallRefresh {
    suspend fun reconcile()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DetectedInstallRefreshModule {
    @Binds
    @Singleton
    fun detectedInstallRefresh(implementation: PackageDetectionReconciler): DetectedInstallRefresh
}
