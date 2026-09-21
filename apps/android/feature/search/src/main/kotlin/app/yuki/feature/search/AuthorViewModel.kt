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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

const val AUTHOR_KEY = "author"

private const val AUTHOR_STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class AuthorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    installedListings: InstalledListings,
    installProgress: InstallProgressStore,
) : ViewModel() {
    val author: String = requireAuthor(savedStateHandle[AUTHOR_KEY])

    private val selectedSort = MutableStateFlow(BrowseSortOption.Default)

    val sort: StateFlow<BrowseSortOption> = selectedSort.asStateFlow()

    val installs: StateFlow<ListingInstalls> = observeListingInstalls(
        installedIds = installedListings.observeInstalledIds(),
        activeStates = installProgress.observeActiveStates(),
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(AUTHOR_STOP_TIMEOUT_MILLIS),
        initialValue = ListingInstalls(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val listings: Flow<PagingData<ListingSummary>> = selectedSort
        .flatMapLatest { active -> pagerFor(BrowseFilter(sort = active, author = author)) }
        .cachedIn(viewModelScope)

    fun onSortChange(value: BrowseSortOption) {
        selectedSort.value = value
    }

    private fun pagerFor(filter: BrowseFilter): Flow<PagingData<ListingSummary>> =
        Pager(config = PagingConfig(pageSize = BROWSE_PAGE_SIZE)) {
            ListingPagingSource(repository = repository, filter = filter)
        }.flow
}

private fun requireAuthor(raw: String?): String {
    val author = raw?.trim().orEmpty()
    return author.ifEmpty { error("Author screen opened without an author: $raw") }
}
