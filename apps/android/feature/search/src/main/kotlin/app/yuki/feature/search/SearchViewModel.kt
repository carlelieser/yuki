package app.yuki.feature.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.yuki.core.datastore.RecentSearchStore
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import app.yuki.core.model.readListingCategory
import app.yuki.core.model.toUiState
import app.yuki.core.network.ListingRepository
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val SEARCH_DEBOUNCE_MILLIS = 250L
const val MINIMUM_QUERY_LENGTH = 1
const val MAXIMUM_QUERY_LENGTH = 100
const val SEARCH_CATEGORY_KEY = "category"

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
    private val recentSearches: RecentSearchStore,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val filter = MutableStateFlow(
        BrowseFilter(category = readListingCategory(savedStateHandle[SEARCH_CATEGORY_KEY])),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val listings: Flow<PagingData<ListingSummary>> = filter
        .flatMapLatest { active -> pagerFor(active) }
        .cachedIn(viewModelScope)

    val state: StateFlow<UiState<SearchContent>> =
        combine(query, recentSearches.recentSearches, results(), filter, ::content)
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
        query.debounce(SEARCH_DEBOUNCE_MILLIS)
            .flatMapLatest { value -> resultsFor(normalize(value)) }

    private fun resultsFor(value: String): Flow<UiState<List<ListingSummary>>?> {
        if (value.length < MINIMUM_QUERY_LENGTH) return flowOf(null)

        return flow {
            emit(UiState.Loading)
            val outcome = repository.search(value).toUiState()
            if (outcome is UiState.Success) recentSearches.remember(value)
            emit(outcome)
        }
    }
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

private fun normalize(query: String): String = query.trim().take(MAXIMUM_QUERY_LENGTH)

private fun content(
    query: String,
    recent: List<String>,
    results: UiState<List<ListingSummary>>?,
    filter: BrowseFilter,
): UiState<SearchContent> = UiState.Success(
    SearchContent(query = query, recent = recent, results = results, filter = filter),
)
