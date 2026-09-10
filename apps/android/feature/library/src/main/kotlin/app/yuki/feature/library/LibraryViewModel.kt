package app.yuki.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.model.InstallState
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LibraryViewModel @Inject internal constructor(
    store: InstallStore,
    private val progress: InstallProgressStore,
    private val dependencies: LibraryDependencies,
) : ViewModel() {
    private val resumes = MutableStateFlow(0)

    val state: StateFlow<UiState<LibraryContent>> =
        combine(presentInstalls(store), progress.observeActive()) { installs, active ->
            UiState.Success(LibraryContent(installs.map { app -> toItem(app, active) }))
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading,
        )

    fun onResume() {
        resumes.value += 1
    }

    private fun presentInstalls(store: InstallStore): Flow<List<InstalledApp>> =
        combine(store.observeInstalls(), resumes) { installs, _ -> installs }
            .map { installs -> dependencies.reconciler.reconcile(installs) }

    private fun toItem(app: InstalledApp, active: List<InstallProgress>): LibraryItem = LibraryItem(
        app = app,
        canOpen = dependencies.packages.launchIntentExists(app.packageName),
        install = active.stateFor(app.githubRepoId),
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

private fun List<InstallProgress>.stateFor(githubRepoId: Long): InstallState =
    firstOrNull { progress -> progress.githubRepoId == githubRepoId }?.state
        ?: InstallState.NotInstalled
