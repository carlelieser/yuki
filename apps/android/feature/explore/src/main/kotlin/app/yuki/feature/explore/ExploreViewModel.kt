package app.yuki.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallProgressStore
import app.yuki.core.installer.InstalledListings
import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

const val SECTION_ITEM_COUNT = 3
const val ROW_ITEM_COUNT = 5

private const val NEWEST_SORT = "newest"
private const val UPDATED_SORT = "updated"

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ListingRepository,
    installedListings: InstalledListings,
    installProgress: InstallProgressStore,
) : ViewModel() {
    private val featured = MutableStateFlow<UiState<List<ListingSummary>>>(UiState.Loading)
    private val newest = MutableStateFlow<UiState<List<ListingSummary>>>(UiState.Loading)
    private val updated = MutableStateFlow<UiState<List<ListingSummary>>>(UiState.Loading)
    private val sections = MutableStateFlow<UiState<List<CategorySection>>>(UiState.Loading)
    private val refreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = refreshing.asStateFlow()

    val state: StateFlow<UiState<ExploreContent>> =
        combine(
            combine(featured, newest, updated, ::ExploreRows),
            sections,
            installedListings.observeInstalledIds(),
            installProgress.observeActive(),
            ::content,
        )
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading,
            )

    init {
        refresh()
    }

    fun refresh() {
        refreshFeatured()
        refreshSections()
        refreshRows()
    }

    fun refreshFeatured() {
        viewModelScope.launch {
            featured.value = UiState.Loading
            loadFeatured()
        }
    }

    fun refreshSections() {
        viewModelScope.launch {
            sections.value = UiState.Loading
            loadSections()
        }
    }

    fun refreshRows() {
        viewModelScope.launch {
            newest.value = UiState.Loading
            updated.value = UiState.Loading
            awaitAll(
                async { loadRow(newest, NEWEST_SORT) },
                async { loadRow(updated, UPDATED_SORT) },
            )
        }
    }

    fun onPullToRefresh() {
        if (refreshing.value) return

        refreshing.value = true
        viewModelScope.launch {
            try {
                awaitAll(
                    async { loadFeatured(keepsContentOnFailure = true) },
                    async { loadSections(keepsContentOnFailure = true) },
                    async { loadRow(newest, NEWEST_SORT, keepsContentOnFailure = true) },
                    async { loadRow(updated, UPDATED_SORT, keepsContentOnFailure = true) },
                )
            } finally {
                refreshing.value = false
            }
        }
    }

    private suspend fun loadFeatured(keepsContentOnFailure: Boolean = false) {
        val outcome = repository.featured().toUiState()
        featured.value = featured.value.replacedBy(outcome, keepsContentOnFailure)
    }

    private suspend fun loadSections(keepsContentOnFailure: Boolean = false) {
        val outcome = repository.sections(SECTION_ITEM_COUNT).toUiState()
        sections.value = sections.value.replacedBy(outcome, keepsContentOnFailure)
    }

    private suspend fun loadRow(
        row: MutableStateFlow<UiState<List<ListingSummary>>>,
        sort: String,
        keepsContentOnFailure: Boolean = false,
    ) {
        val query = BrowseQuery(sort = sort, limit = ROW_ITEM_COUNT)
        val outcome = repository.browse(query).map { page -> page.results }.toUiState()
        row.value = row.value.replacedBy(outcome, keepsContentOnFailure)
    }
}

private fun <T> UiState<T>.replacedBy(
    outcome: UiState<T>,
    keepsContentOnFailure: Boolean,
): UiState<T> {
    val keepsPrevious = keepsContentOnFailure &&
        outcome is UiState.Failure &&
        this is UiState.Success

    return if (keepsPrevious) this else outcome
}

private const val STOP_TIMEOUT_MILLIS = 5_000L

private fun content(
    rows: ExploreRows,
    sections: UiState<List<CategorySection>>,
    installedIds: Set<Long>,
    active: List<InstallProgress>,
): UiState<ExploreContent> {
    val loaded = ExploreContent(
        featured = rows.featured,
        newest = rows.newest,
        updated = rows.updated,
        sections = sections,
        installedIds = installedIds,
        installStates = active.associate { progress -> progress.githubRepoId to progress.state },
    )

    val sources = listOf(rows.featured, rows.newest, rows.updated, sections)
    if (sources.any { source -> source is UiState.Success }) return UiState.Success(loaded)
    if (sources.any { source -> source is UiState.Loading }) return UiState.Loading

    val failure = sources.firstNotNullOf { source -> source as? UiState.Failure }
    return UiState.Failure(failure.reason)
}
