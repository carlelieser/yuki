package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.UiState
import app.yuki.core.model.failureReason
import app.yuki.core.network.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ListingReviewsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
    sessionStore: SessionStore,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ListingReviewsViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    private val mutableReviews = MutableStateFlow<UiState<ListingReviews>>(UiState.Loading)

    val reviews: StateFlow<UiState<ListingReviews>> = mutableReviews.asStateFlow()

    private val ownReloads = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val viewer: StateFlow<ReviewViewer> = combine(
        sessionStore.session.map { session -> session != null }.distinctUntilChanged(),
        ownReloads,
    ) { isSignedIn, _ -> isSignedIn }
        .mapLatest(::viewerFor)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = ReviewViewer(isSignedIn = false),
        )

    init {
        refresh()
        viewModelScope.launch {
            repository.changes.filter { changed -> changed == slug }.collect {
                refresh()
                ownReloads.update { count -> count + 1 }
            }
        }
    }

    fun refresh() {
        mutableReviews.value = UiState.Loading
        viewModelScope.launch { mutableReviews.value = loadReviews() }
    }

    private suspend fun loadReviews(): UiState<ListingReviews> {
        val summary = viewModelScope.async { repository.summary(slug) }
        val page = repository.reviews(slug, offset = 0)
        val loadedSummary = summary.await()

        val failure = loadedSummary.exceptionOrNull() ?: page.exceptionOrNull()
        if (failure != null) return UiState.Failure(failure.failureReason())

        val reviews = page.getOrThrow()
        return UiState.Success(
            ListingReviews(
                summary = loadedSummary.getOrThrow(),
                preview = reviews.results.take(REVIEW_PREVIEW_COUNT),
                hasMore = reviews.hasMore || reviews.results.size > REVIEW_PREVIEW_COUNT,
            ),
        )
    }

    private suspend fun viewerFor(isSignedIn: Boolean): ReviewViewer {
        if (!isSignedIn) return ReviewViewer(isSignedIn = false)

        return ReviewViewer(isSignedIn = true, own = repository.ownReview(slug).getOrNull())
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
