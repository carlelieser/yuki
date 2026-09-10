package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
import app.yuki.core.network.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val LISTING_SLUG_KEY = "slug"

@HiltViewModel
class ListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    private val installGateway: ListingInstallGateway,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ListingViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    private val mutableListing = MutableStateFlow<UiState<ListingUiModel>>(UiState.Loading)

    val listing: StateFlow<UiState<ListingUiModel>> = mutableListing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val installState: StateFlow<InstallState> = mutableListing
        .flatMapLatest(::installStateFor)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = InstallState.NotInstalled,
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
            InstallAction.Cancel -> viewModelScope.launch { installGateway.cancel(model.repoId) }
            InstallAction.Open -> viewModelScope.launch { installGateway.open(model.repoId) }
        }
    }

    private fun startInstall(model: ListingUiModel) {
        val version = model.installableVersion ?: return
        val request = ListingInstallRequest(detail = model.detail, version = version)

        viewModelScope.launch { installGateway.install(request) }
    }

    private fun installStateFor(state: UiState<ListingUiModel>): Flow<InstallState> =
        when (state) {
            is UiState.Success -> installGateway.observe(state.data.repoId)
            else -> flowOf(InstallState.NotInstalled)
        }

    private fun successOrNull(): ListingUiModel? =
        (mutableListing.value as? UiState.Success)?.data

    private val ListingUiModel.repoId: Long get() = detail.githubRepoId

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
