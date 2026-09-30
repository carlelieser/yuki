package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
import app.yuki.core.network.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

const val REVIEWED_TITLE_KEY = "title"
const val REVIEWED_ICON_KEY = "iconUrl"

data class ReviewedListing(
    val title: String,
    val iconUrl: String?,
)

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ReviewsViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    val listing = ReviewedListing(
        title = savedStateHandle[REVIEWED_TITLE_KEY] ?: "",
        iconUrl = savedStateHandle[REVIEWED_ICON_KEY],
    )

    private val mutableSummary = MutableStateFlow<UiState<RatingSummary>>(UiState.Loading)

    val summary: StateFlow<UiState<RatingSummary>> = mutableSummary.asStateFlow()

    private val generation = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val reviews: Flow<PagingData<Review>> = generation
        .flatMapLatest {
            Pager(config = PagingConfig(pageSize = REVIEWS_PAGE_SIZE)) {
                ReviewPagingSource(repository = repository, slug = slug)
            }.flow
        }
        .cachedIn(viewModelScope)

    init {
        refreshSummary()
        viewModelScope.launch {
            repository.changes.filter { changed -> changed == slug }.collect {
                refreshSummary()
                generation.update { count -> count + 1 }
            }
        }
    }

    fun refreshSummary() {
        mutableSummary.value = UiState.Loading
        viewModelScope.launch { mutableSummary.value = repository.summary(slug).toUiState() }
    }
}
