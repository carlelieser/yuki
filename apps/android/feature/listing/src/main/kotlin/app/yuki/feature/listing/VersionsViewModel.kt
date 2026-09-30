package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class VersionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    private val installGateway: ListingInstallGateway,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "VersionsViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    private val installDispatch = VersionInstallDispatch(installGateway)

    private val mutableListing = MutableStateFlow<UiState<ListingDetail>>(UiState.Loading)

    val listing: StateFlow<UiState<ListingDetail>> = mutableListing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val installStatus: StateFlow<ListingInstallStatus?> = mutableListing
        .flatMapLatest(::installStatusFor)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null,
        )

    init {
        refresh()
    }

    fun refresh() {
        mutableListing.value = UiState.Loading
        viewModelScope.launch {
            mutableListing.value = repository.detail(slug).toUiState()
        }
    }

    fun onVersionInstallAction(action: InstallAction, version: InstallableVersion) {
        val detail = (mutableListing.value as? UiState.Success)?.data ?: return
        val request = ListingInstallRequest(detail = detail, version = version)

        viewModelScope.launch { installDispatch.dispatch(action, request) }
    }

    private fun installStatusFor(state: UiState<ListingDetail>): Flow<ListingInstallStatus?> =
        when (state) {
            is UiState.Success -> installGateway.observe(state.data.githubRepoId)
            is UiState.Loading -> emptyFlow()
            is UiState.Failure -> flowOf(IDLE_STATUS)
        }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L

        val IDLE_STATUS = ListingInstallStatus(InstallState.NotInstalled, versionTag = null)
    }
}
