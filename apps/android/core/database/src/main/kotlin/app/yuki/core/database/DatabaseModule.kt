package app.yuki.core.database

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseProviders {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): YukiDatabase =
        Room.databaseBuilder(context, YukiDatabase::class.java, YUKI_DATABASE_NAME).build()

    @Provides
    @Singleton
    fun installDao(database: YukiDatabase): InstallDao = database.installDao()
}

@Module
@InstallIn(SingletonComponent::class)
internal interface DatabaseBindings {
    @Binds
    @Singleton
    fun installStore(implementation: RoomInstallStore): InstallStore
}
