package app.yuki.feature.updates

import kotlinx.coroutines.flow.Flow

fun interface AutoUpdateCheckPreference {
    fun isEnabled(): Flow<Boolean>
}
