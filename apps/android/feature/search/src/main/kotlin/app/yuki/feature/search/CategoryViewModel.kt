package app.yuki.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.yuki.core.installer.InstalledListings
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.readListingCategory
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
import kotlinx.coroutines.flow.stateIn

const val CATEGORY_KEY = "category"

private const val CATEGORY_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class CategoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    installedListings: InstalledListings,
) : ViewModel() {
    val category: ListingCategory = requireCategory(savedStateHandle[CATEGORY_KEY])

    private val selectedSort = MutableStateFlow(BrowseSortOption.Default)

    val sort: StateFlow<BrowseSortOption> = selectedSort.asStateFlow()

    val installedIds: StateFlow<Set<Long>> = installedListings.observeInstalledIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(CATEGORY_STOP_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val listings: Flow<PagingData<ListingSummary>> = selectedSort
        .flatMapLatest { active -> pagerFor(BrowseFilter(sort = active, category = category)) }
        .cachedIn(viewModelScope)

    fun onSortChange(value: BrowseSortOption) {
        selectedSort.value = value
    }

    private fun pagerFor(filter: BrowseFilter): Flow<PagingData<ListingSummary>> =
        Pager(config = PagingConfig(pageSize = BROWSE_PAGE_SIZE)) {
            ListingPagingSource(repository = repository, filter = filter)
        }.flow
}

private fun requireCategory(raw: String?): ListingCategory =
    readListingCategory(raw)
        ?: error("Category screen opened with an unknown category: $raw")
