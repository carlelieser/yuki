package app.yuki.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject internal constructor(
    private val store: InstallStore,
    private val progress: InstallProgressStore,
    private val dependencies: LibraryDependencies,
) : ViewModel() {
    private val refreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = refreshing.asStateFlow()

    val state: StateFlow<UiState<LibraryContent>> =
        combine(presentInstalls(), activeProgress()) { installed, active ->
            UiState.Success(LibraryContent(merge(installed, active)))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    init {
        viewModelScope.launch { refreshLocalState() }
    }

    fun onPullToRefresh() {
        if (refreshing.value) return

        refreshing.value = true
        viewModelScope.launch {
            try {
                reconcile()
                delay(MINIMUM_REFRESH_MILLIS)
            } finally {
                refreshing.value = false
            }
        }
    }

    fun onDismiss(githubRepoId: Long) {
        viewModelScope.launch { progress.clear(githubRepoId) }
    }

    private suspend fun reconcile() {
        runCatching { dependencies.detection.reconcile() }
        refreshLocalState()
    }

    private suspend fun refreshLocalState() {
        dependencies.reconciler.reconcile(store.installs())
        progress.clearSettled()
    }

    private fun presentInstalls(): Flow<List<InstalledApp>> =
        combine(store.observeInstalls(), dependencies.packages.observeChanges()) { installs, _ ->
            installs.filter { app -> dependencies.packages.isPresent(app.packageName) }
        }

    private fun activeProgress() = progress.observeActive().onStart {
        progress.clearSettled()
    }

    private fun merge(installed: List<InstalledApp>, active: List<InstallProgress>) = mergeLibrary(
        input = LibraryMergeInput(installed, active),
        canOpen = dependencies.packages::launchIntentExists,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MINIMUM_REFRESH_MILLIS = 400L
    }
}
