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

    val isSyncing: StateFlow<Boolean> = syncing.asStateFlow()

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
            try {
                sync.run()
            } finally {
                syncing.value = false
            }
        }
    }
}

private const val SUBSCRIPTION_TIMEOUT = 5_000L
