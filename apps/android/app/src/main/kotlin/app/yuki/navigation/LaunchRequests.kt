package app.yuki.navigation

import android.content.Intent
import app.yuki.feature.updates.EXTRA_OPEN_UPDATES
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LaunchRequests {
    private val pending = MutableStateFlow<YukiTab?>(null)

    internal val requestedTab: StateFlow<YukiTab?> = pending.asStateFlow()

    fun handle(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATES, false) != true) return

        intent.removeExtra(EXTRA_OPEN_UPDATES)
        pending.value = YukiTab.Updates
    }

    internal fun consume() {
        pending.value = null
    }
}
