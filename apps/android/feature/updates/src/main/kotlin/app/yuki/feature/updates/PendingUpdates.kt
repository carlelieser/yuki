package app.yuki.feature.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.database.InstallStore
import app.yuki.core.database.PendingUpdate
import app.yuki.core.database.PendingUpdateStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.isNewerTag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

internal class OutstandingUpdates @Inject constructor(
    private val store: InstallStore,
    private val pending: PendingUpdateStore,
) {
    fun observe(): Flow<List<InstalledApp>> =
        combine(pending.observe(), store.observeInstalls(), ::outstandingUpdates)
            .distinctUntilChanged()
}

@HiltViewModel
class PendingUpdatesViewModel @Inject internal constructor(
    outstanding: OutstandingUpdates,
) : ViewModel() {
    val count: StateFlow<Int> = outstanding.observe().map { apps -> apps.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = 0,
        )
}

internal fun outstandingUpdates(
    pending: List<PendingUpdate>,
    installs: List<InstalledApp>,
): List<InstalledApp> {
    val newestTags = pending.associate { update -> update.githubRepoId to update.versionTag }

    return installs.filter { app ->
        val tag = newestTags[app.githubRepoId] ?: return@filter false
        isNewerTag(tag, app.versionTag)
    }
}
