package app.yuki.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.observeListingInstalls
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstalledListings
import app.yuki.core.installer.observeActiveStates
import app.yuki.core.model.ListingSummary
import app.yuki.core.network.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

const val CATALOG_LIST_KEY = "list"

private const val CATALOG_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class CatalogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    installedListings: InstalledListings,
    installProgress: InstallProgressStore,
) : ViewModel() {
    val list: CatalogList = requireList(savedStateHandle[CATALOG_LIST_KEY])

    val installs: StateFlow<ListingInstalls> = observeListingInstalls(
        installedIds = installedListings.observeInstalledIds(),
        activeStates = installProgress.observeActiveStates(),
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(CATALOG_STOP_TIMEOUT_MILLIS),
        initialValue = ListingInstalls(),
    )

    val listings: Flow<PagingData<ListingSummary>> =
        Pager(config = PagingConfig(pageSize = BROWSE_PAGE_SIZE)) {
            ListingPagingSource(repository = repository, filter = BrowseFilter(sort = list.sort))
        }.flow.cachedIn(viewModelScope)
}

private fun requireList(raw: String?): CatalogList =
    readCatalogList(raw) ?: error("Catalog screen opened with an unknown list: $raw")
