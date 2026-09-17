package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingVersion
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
import app.yuki.core.network.ListingRepository
import app.yuki.core.network.YukiBaseUrl
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val LISTING_SLUG_KEY = "slug"

@HiltViewModel
class ListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    private val installGateway: ListingInstallGateway,
    @param:YukiBaseUrl private val baseUrl: String,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ListingViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    val shareUrl: String get() = listingShareUrl(baseUrl, slug)

    private val mutableListing = MutableStateFlow<UiState<ListingUiModel>>(UiState.Loading)

    val listing: StateFlow<UiState<ListingUiModel>> = mutableListing.asStateFlow()

    private val mutableConfirmingUninstall = MutableStateFlow(false)

    val isConfirmingUninstall: StateFlow<Boolean> = mutableConfirmingUninstall.asStateFlow()

    private val mutableUninstallFailed = MutableStateFlow(false)

    val hasUninstallFailed: StateFlow<Boolean> = mutableUninstallFailed.asStateFlow()

    val isShareable: StateFlow<Boolean> = mutableListing
        .map { listing -> listing is UiState.Success }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false,
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val installStatus: StateFlow<ListingInstallStatus> = mutableListing
        .flatMapLatest(::installStatusFor)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = IDLE_STATUS,
        )

    init {
        refresh()
    }

    fun refresh() {
        mutableListing.value = UiState.Loading
        viewModelScope.launch {
            val detail = repository.detail(slug)
            mutableListing.value = detail.map { it.toUiModel() }.toUiState()
        }
    }

    fun onInstallAction(action: InstallAction) {
        val model = successOrNull() ?: return

        when (action) {
            InstallAction.Install, InstallAction.Update, InstallAction.Retry -> startInstall(model)
            InstallAction.Cancel, InstallAction.Dismiss ->
                viewModelScope.launch { installGateway.cancel(model.repoId) }
            InstallAction.Open -> viewModelScope.launch { installGateway.open(model.repoId) }
            InstallAction.Uninstall -> requestUninstall(model)
        }
    }

    fun onVersionInstallAction(action: InstallAction, version: ListingVersion) {
        val model = successOrNull() ?: return

        when (action) {
            InstallAction.Install, InstallAction.Update, InstallAction.Retry ->
                startInstall(model, version)
            InstallAction.Cancel, InstallAction.Dismiss ->
                viewModelScope.launch { installGateway.cancel(model.repoId) }
            InstallAction.Open -> viewModelScope.launch { installGateway.open(model.repoId) }
            InstallAction.Uninstall -> Unit
        }
    }

    fun onUninstallConfirmed() {
        val model = successOrNull() ?: return

        mutableConfirmingUninstall.value = false
        viewModelScope.launch { uninstall(model) }
    }

    fun onUninstallDismissed() {
        mutableConfirmingUninstall.value = false
    }

    fun onResumed() {
        installGateway.refresh()
    }

    private fun requestUninstall(model: ListingUiModel) {
        viewModelScope.launch {
            if (installGateway.isSilentUninstall()) {
                mutableConfirmingUninstall.value = true
                return@launch
            }

            uninstall(model)
        }
    }

    private suspend fun uninstall(model: ListingUiModel) {
        mutableUninstallFailed.value = false

        runCatching { installGateway.uninstall(model.repoId) }
            .onFailure { error ->
                if (error is CancellationException) throw error
                mutableUninstallFailed.value = true
                installGateway.refresh()
            }
    }

    private fun startInstall(model: ListingUiModel) {
        val version = model.installableVersion ?: return

        startInstall(model, version)
    }

    private fun startInstall(model: ListingUiModel, version: ListingVersion) {
        if (version.downloadUrl == null) return

        val request = ListingInstallRequest(detail = model.detail, version = version)

        viewModelScope.launch { installGateway.install(request) }
    }

    private fun installStatusFor(
        state: UiState<ListingUiModel>,
    ): Flow<ListingInstallStatus> = when (state) {
        is UiState.Success -> installGateway.observe(state.data.repoId)
        else -> flowOf(IDLE_STATUS)
    }

    private fun successOrNull(): ListingUiModel? =
        (mutableListing.value as? UiState.Success)?.data

    private val ListingUiModel.repoId: Long get() = detail.githubRepoId

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        val IDLE_STATUS = ListingInstallStatus(InstallState.NotInstalled, versionTag = null)
    }
}
