package app.yuki.feature.settings

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface YukiPreferenceReader {
    fun includePrereleases(): Flow<Boolean>

    fun installMode(): Flow<InstallMode>

    fun isDynamicColorEnabled(): Flow<Boolean>
}

@Singleton
internal class StoreBackedPreferenceReader @Inject constructor(
    private val store: PreferenceStore,
) : YukiPreferenceReader {
    override fun includePrereleases(): Flow<Boolean> = read(YukiPreferences::includePrereleases)

    override fun installMode(): Flow<InstallMode> = read(YukiPreferences::installMode)

    override fun isDynamicColorEnabled(): Flow<Boolean> =
        read(YukiPreferences::isDynamicColorEnabled)

    private fun <T> read(select: (YukiPreferences) -> T): Flow<T> =
        store.preferences.map(select).distinctUntilChanged()
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class YukiPreferenceReaderModule {
    @Binds
    @Singleton
    abstract fun bindReader(reader: StoreBackedPreferenceReader): YukiPreferenceReader
}
