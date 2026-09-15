package app.yuki.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.yuki.core.datastore.RecentSearchStore
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstalledListings
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
import app.yuki.core.network.ListingRepository
import app.yuki.core.network.SearchQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val SEARCH_DEBOUNCE_MILLIS = 250L
const val MINIMUM_QUERY_LENGTH = 1
const val MAXIMUM_QUERY_LENGTH = 100

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ListingRepository,
    private val recentSearches: RecentSearchStore,
    installedListings: InstalledListings,
    installProgress: InstallProgressStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(BrowseFilter())

    val installedIds: StateFlow<Set<Long>> = installedListings.observeInstalledIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptySet(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val listings: Flow<PagingData<ListingSummary>> = filter
        .flatMapLatest { active -> pagerFor(active) }
        .cachedIn(viewModelScope)

    private val installs: Flow<ListingInstalls> =
        combine(installedIds, installProgress.observeActive()) { ids, active ->
            ListingInstalls(
                installedIds = ids,
                installStates = active.associate { it.githubRepoId to it.state },
            )
        }

    val state: StateFlow<UiState<SearchContent>> =
        combine(query, recentSearches.recentSearches, results(), filter, installs, ::content)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onCategoryChange(category: ListingCategory?) {
        filter.value = filter.value.copy(category = category)
    }

    fun onSortChange(sort: BrowseSortOption) {
        filter.value = filter.value.copy(sort = sort)
    }

    fun onRecentSearchRemoved(value: String) {
        viewModelScope.launch { recentSearches.forget(value) }
    }

    private fun pagerFor(active: BrowseFilter): Flow<PagingData<ListingSummary>> =
        Pager(config = PagingConfig(pageSize = BROWSE_PAGE_SIZE)) {
            ListingPagingSource(repository = repository, filter = active)
        }.flow

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun results(): Flow<UiState<List<ListingSummary>>?> =
        combine(query.debounce(SEARCH_DEBOUNCE_MILLIS), filter, ::searchRequest)
            .distinctUntilChanged()
            .flatMapLatest { request -> resultsFor(request) }

    private fun resultsFor(request: SearchQuery): Flow<UiState<List<ListingSummary>>?> {
        if (request.term.length < MINIMUM_QUERY_LENGTH) return flowOf(null)

        return flow {
            emit(UiState.Loading)
            val outcome = repository.search(request).toUiState()
            if (outcome is UiState.Success) recentSearches.remember(request.term)
            emit(outcome)
        }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

private fun normalize(query: String): String = query.trim().take(MAXIMUM_QUERY_LENGTH)

private fun searchRequest(query: String, filter: BrowseFilter): SearchQuery = SearchQuery(
    term = normalize(query),
    sort = filter.sort.key.wireValue,
    order = filter.sort.order.wireValue,
)

private fun content(
    query: String,
    recent: List<String>,
    results: UiState<List<ListingSummary>>?,
    filter: BrowseFilter,
    installs: ListingInstalls,
): UiState<SearchContent> = UiState.Success(
    SearchContent(
        query = query,
        recent = recent,
        results = results,
        filter = filter,
        installedIds = installs.installedIds,
        installStates = installs.installStates,
    ),
)
