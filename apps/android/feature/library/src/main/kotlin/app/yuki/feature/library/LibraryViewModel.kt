package app.yuki.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.LibraryEntry
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
    private val remote = MutableStateFlow(emptyList<LibraryEntry>())
    private val filter = MutableStateFlow(LibraryFilter.All)

    val isRefreshing: StateFlow<Boolean> = refreshing.asStateFlow()

    val state: StateFlow<UiState<LibraryContent>> =
        combine(presentInstalls(), activeProgress(), remote, filter, ::content).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    init {
        viewModelScope.launch { refreshLocalState() }
    }

    fun onEnter() {
        viewModelScope.launch { loadRemoteLibrary() }
    }

    fun onFilterChange(selected: LibraryFilter) {
        filter.value = selected
    }

    fun onPullToRefresh() {
        if (refreshing.value) return

        refreshing.value = true
        viewModelScope.launch {
            try {
                reconcile()
                loadRemoteLibrary()
                delay(MINIMUM_REFRESH_MILLIS)
            } finally {
                refreshing.value = false
            }
        }
    }

    private suspend fun loadRemoteLibrary() {
        remote.value = dependencies.library.entries()
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

    private fun content(
        installed: List<InstalledApp>,
        active: List<InstallProgress>,
        library: List<LibraryEntry>,
        selected: LibraryFilter,
    ): UiState<LibraryContent> = UiState.Success(
        LibraryContent(
            items = mergeLibrary(LibraryMergeInput(installed, library, active)),
            filter = selected,
        ),
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val MINIMUM_REFRESH_MILLIS = 400L
    }
}
