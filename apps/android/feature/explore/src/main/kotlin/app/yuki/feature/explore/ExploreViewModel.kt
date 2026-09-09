package app.yuki.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.yuki.core.datastore.RecentSearchStore
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
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

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ListingRepository,
    private val recentSearches: RecentSearchStore,
) : ViewModel() {
    private val featured = MutableStateFlow<UiState<List<ListingSummary>>>(UiState.Loading)
    private val query = MutableStateFlow("")

    val listings: Flow<PagingData<ListingSummary>> =
        Pager(config = PagingConfig(pageSize = BROWSE_PAGE_SIZE)) {
            ListingPagingSource(repository)
        }.flow.cachedIn(viewModelScope)

    val state: StateFlow<UiState<ExploreContent>> =
        combine(featured, searchState(), ::content)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    init {
        refreshFeatured()
    }

    fun onQueryChange(value: String) {
        query.value = value
    }

    fun onRecentSearchRemoved(value: String) {
        viewModelScope.launch { recentSearches.forget(value) }
    }

    fun refreshFeatured() {
        viewModelScope.launch {
            featured.value = UiState.Loading
            featured.value = repository.featured().toUiState()
        }
    }

    private fun searchState(): Flow<SearchState> =
        combine(query, recentSearches.recentSearches, results()) { value, recent, results ->
            SearchState(query = value, recent = recent, results = results)
        }

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun results(): Flow<UiState<List<ListingSummary>>?> =
        query.debounce(SEARCH_DEBOUNCE_MILLIS).flatMapLatest { value -> resultsFor(normalize(value)) }

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
    featured: UiState<List<ListingSummary>>,
    search: SearchState,
): UiState<ExploreContent> {
    val isBlockedByFeatured = featured !is UiState.Success && !search.isSearching

    if (isBlockedByFeatured && featured is UiState.Failure) return UiState.Failure(featured.reason)
    if (isBlockedByFeatured) return UiState.Loading

    return UiState.Success(ExploreContent(featured = featured, search = search))
}
