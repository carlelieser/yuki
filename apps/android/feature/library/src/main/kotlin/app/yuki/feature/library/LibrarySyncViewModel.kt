package app.yuki.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.auth.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibrarySyncViewModel @Inject internal constructor(
    private val sync: LibrarySync,
    store: SessionStore,
) : ViewModel() {
    private val syncing = MutableStateFlow(false)
    private val messages = MutableStateFlow<SyncMessage?>(null)

    val isSyncing: StateFlow<Boolean> = syncing.asStateFlow()

    val message: StateFlow<SyncMessage?> = messages.asStateFlow()

    val isSignedIn: StateFlow<Boolean> = store.session
        .map { session -> session != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT),
            initialValue = false,
        )

    fun onSync() {
        if (syncing.value) return

        syncing.value = true

        viewModelScope.launch {
            val outcome = runCatching { sync.run() }

            syncing.value = false
            messages.value = outcome.fold(
                onSuccess = ::syncMessage,
                onFailure = { SyncMessage.Failed },
            )
        }
    }

    fun onMessageShown() {
        messages.value = null
    }
}

internal fun syncMessage(result: LibrarySyncResult): SyncMessage = when {
    result.failed > 0 -> SyncMessage.Partial(uploaded = result.uploaded, failed = result.failed)
    result.uploaded > 0 -> SyncMessage.Added(uploaded = result.uploaded)
    else -> SyncMessage.UpToDate
}

private const val SUBSCRIPTION_TIMEOUT = 5_000L
