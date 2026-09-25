package app.yuki.feature.updates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.database.InstallStore
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.AvailableUpdate
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UpdatesViewModel @Inject internal constructor(
    private val store: InstallStore,
    private val dependencies: UpdatesDependencies,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<UpdatesContent>>(UiState.Loading)
    private val installs = MutableStateFlow<Map<Long, InstallState>>(emptyMap())
    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<UiState<UpdatesContent>> = mutableState.asStateFlow()

    val isRefreshing: StateFlow<Boolean> = refreshing.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            dependencies.installer.observeActiveStates().collect(::publish)
        }
    }

    fun refresh() {
        mutableState.value = UiState.Loading
        viewModelScope.launch { load() }
    }

    fun onPullToRefresh() {
        if (refreshing.value) return

        refreshing.value = true
        viewModelScope.launch {
            try {
                load()
            } finally {
                refreshing.value = false
            }
        }
    }

    private suspend fun load() {
        val includePrereleases = dependencies.preference.includePrereleases().first()
        val checked = dependencies.check.run(store.installs(), includePrereleases)
        mutableState.value = UiState.Success(checked.withInstallStates(installs.value))
    }

    fun onInstallAction(githubRepoId: Long, action: InstallAction) {
        val update = updateFor(githubRepoId) ?: return

        when (action) {
            InstallAction.Update, InstallAction.Retry, InstallAction.Install ->
                dependencies.installer.install(update)
            InstallAction.Cancel, InstallAction.Dismiss -> cancel(githubRepoId)
            InstallAction.Open, InstallAction.Uninstall -> Unit
        }
    }

    private fun cancel(githubRepoId: Long) {
        viewModelScope.launch { dependencies.installer.cancel(githubRepoId) }
    }

    private fun publish(active: Map<Long, InstallState>) {
        installs.value = active
        mutableState.update { current -> current.mapContent(active) }
    }

    private fun updateFor(githubRepoId: Long): AvailableUpdate? = contentOrNull()
        ?.updates
        ?.firstOrNull { row -> row.githubRepoId == githubRepoId }
        ?.update

    private fun contentOrNull(): UpdatesContent? = (mutableState.value as? UiState.Success)?.data
}

private fun UiState<UpdatesContent>.mapContent(
    installs: Map<Long, InstallState>,
): UiState<UpdatesContent> = when (this) {
    is UiState.Success -> UiState.Success(data.withInstallStates(installs))
    else -> this
}

private fun UpdatesContent.withInstallStates(
    installs: Map<Long, InstallState>,
): UpdatesContent = copy(
    updates = updates.map { row ->
        row.copy(install = installs[row.githubRepoId].forUpdate(row.update))
    },
)

private fun InstallState?.forUpdate(update: AvailableUpdate): InstallState {
    val isEarlierInstall = this is InstallState.Installed && versionTag != update.version.tag

    return if (this == null || isEarlierInstall) update.toInstallState() else this
}
