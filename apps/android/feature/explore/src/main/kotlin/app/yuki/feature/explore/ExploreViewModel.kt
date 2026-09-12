package app.yuki.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
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

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val repository: ListingRepository,
) : ViewModel() {
    private val featured = MutableStateFlow<UiState<List<ListingSummary>>>(UiState.Loading)
    private val sections = MutableStateFlow<UiState<List<CategorySection>>>(UiState.Loading)
    private val refreshing = MutableStateFlow(false)

    val isRefreshing: StateFlow<Boolean> = refreshing.asStateFlow()

    val state: StateFlow<UiState<ExploreContent>> =
        combine(featured, sections, ::content)
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

    fun onPullToRefresh() {
        if (refreshing.value) return

        refreshing.value = true
        viewModelScope.launch {
            try {
                awaitAll(
                    async { loadFeatured(keepsContentOnFailure = true) },
                    async { loadSections(keepsContentOnFailure = true) },
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
    featured: UiState<List<ListingSummary>>,
    sections: UiState<List<CategorySection>>,
): UiState<ExploreContent> {
    val loaded = ExploreContent(featured = featured, sections = sections)

    val hasContent = featured is UiState.Success || sections is UiState.Success
    if (hasContent) return UiState.Success(loaded)

    val blockingFailure = firstFailure(featured, sections)
    if (blockingFailure != null) return UiState.Failure(blockingFailure.reason)

    return UiState.Loading
}

private fun firstFailure(
    featured: UiState<List<ListingSummary>>,
    sections: UiState<List<CategorySection>>,
): UiState.Failure? {
    val isEitherLoading = featured is UiState.Loading || sections is UiState.Loading
    if (isEitherLoading) return null

    return featured as? UiState.Failure ?: sections as? UiState.Failure
}
