package app.yuki.feature.updates

import kotlinx.coroutines.flow.Flow

fun interface PrereleasePreference {
    fun includePrereleases(): Flow<Boolean>
}
