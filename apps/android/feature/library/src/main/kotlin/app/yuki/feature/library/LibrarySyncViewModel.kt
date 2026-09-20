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
    private val messages = MutableStateFlow<String?>(null)

    val isSyncing: StateFlow<Boolean> = syncing.asStateFlow()

    val message: StateFlow<String?> = messages.asStateFlow()

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
                onFailure = { SYNC_FAILED },
            )
        }
    }

    fun onMessageShown() {
        messages.value = null
    }
}

internal const val SYNC_FAILED = "Could not sync your library. Try again."
internal const val SYNC_UP_TO_DATE = "Your library is up to date."

internal fun syncMessage(result: LibrarySyncResult): String = when {
    result.failed > 0 -> "Added ${result.uploaded}, but ${result.failed} could not be synced."
    result.uploaded > 0 -> "Added ${result.uploaded} to your library."
    else -> SYNC_UP_TO_DATE
}

private const val SUBSCRIPTION_TIMEOUT = 5_000L
