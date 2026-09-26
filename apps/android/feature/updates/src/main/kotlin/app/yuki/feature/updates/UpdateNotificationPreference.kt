package app.yuki.feature.updates

import kotlinx.coroutines.flow.Flow

fun interface UpdateNotificationPreference {
    fun isEnabled(): Flow<Boolean>
}
