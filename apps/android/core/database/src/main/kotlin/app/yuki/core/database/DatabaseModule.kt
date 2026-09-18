package app.yuki.core.database

import android.content.Context
import androidx.room.Room
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstallRecorder
import app.yuki.core.installer.LocalInstallRecorder
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseProviders {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): YukiDatabase =
        Room.databaseBuilder(context, YukiDatabase::class.java, YUKI_DATABASE_NAME)
            .addMigrations(*YUKI_MIGRATIONS)
            .build()

    @Provides
    @Singleton
    fun installDao(database: YukiDatabase): InstallDao = database.installDao()

    @Provides
    @Singleton
    fun installProgressDao(database: YukiDatabase): InstallProgressDao =
        database.installProgressDao()

    @Provides
    @Singleton
    fun packageIndexDao(database: YukiDatabase): PackageIndexDao = database.packageIndexDao()

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemUTC()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DatabaseBindings {
    @Binds
    @Singleton
    fun installStore(implementation: RoomInstallStore): InstallStore

    @Binds
    @Singleton
    @LocalInstallRecorder
    fun installRecorder(implementation: RoomInstallRecorder): InstallRecorder

    @Binds
    @Singleton
    fun installProgressStore(implementation: RoomInstallProgressStore): InstallProgressStore

    @Binds
    @Singleton
    fun packageIndexStore(implementation: RoomPackageIndexStore): PackageIndexStore
}
