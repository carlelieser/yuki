package app.yuki.feature.updates

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

private data class UpdateCheckSettings(
    val isAutomatic: Boolean,
    val includePrereleases: Boolean,
)

@Singleton
class UpdateCheckScheduler @Inject internal constructor(
    private val work: UpdateCheckWork,
    private val automatic: AutoUpdateCheckPreference,
    private val prereleases: PrereleasePreference,
) {
    suspend fun follow() {
        var previous: UpdateCheckSettings? = null

        settings().collect { current ->
            apply(previous, current)
            previous = current
        }
    }

    private fun settings(): Flow<UpdateCheckSettings> = combine(
        automatic.isEnabled(),
        prereleases.includePrereleases(),
        ::UpdateCheckSettings,
    ).distinctUntilChanged()

    private suspend fun apply(previous: UpdateCheckSettings?, current: UpdateCheckSettings) {
        if (!current.isAutomatic) {
            work.cancel()
            return
        }

        work.schedule()
        val hasPrereleaseChoiceChanged =
            previous != null && previous.includePrereleases != current.includePrereleases
        if (hasPrereleaseChoiceChanged) work.runOnce()
    }
}
