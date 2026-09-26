package app.yuki.feature.updates

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot

@Singleton
class UpdateNotificationSwitch @Inject internal constructor(
    private val preference: UpdateNotificationPreference,
    private val notifier: UpdateNotifier,
) {
    suspend fun follow() {
        preference.isEnabled()
            .distinctUntilChanged()
            .filterNot { isEnabled -> isEnabled }
            .collect { notifier.dismiss() }
    }
}
