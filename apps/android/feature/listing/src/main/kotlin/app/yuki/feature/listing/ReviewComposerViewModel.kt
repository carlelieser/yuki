package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.FailureReason
import app.yuki.core.model.MAX_REVIEW_BODY
import app.yuki.core.model.OwnReview
import app.yuki.core.model.ReviewDraft
import app.yuki.core.model.failureReason
import app.yuki.core.network.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReviewComposerState(
    val rating: Int = 0,
    val body: String = "",
    val isEditing: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val isRatingMissing: Boolean = false,
    val isConfirmingDelete: Boolean = false,
    val isDone: Boolean = false,
    val failure: FailureReason? = null,
)

@HiltViewModel
class ReviewComposerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReviewRepository,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ReviewComposerViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    private val mutableState = MutableStateFlow(ReviewComposerState())

    val state: StateFlow<ReviewComposerState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val own = repository.ownReview(slug)
            mutableState.update { current ->
                own.fold(
                    onSuccess = { loaded -> current.prefilledFrom(loaded) },
                    onFailure = { error -> current.copy(isLoading = false, failure = error.failureReason()) },
                )
            }
        }
    }

    fun onRatingChange(rating: Int) {
        mutableState.update { current -> current.copy(rating = rating, isRatingMissing = false) }
    }

    fun onBodyChange(body: String) {
        mutableState.update { current -> current.copy(body = body.take(MAX_REVIEW_BODY)) }
    }

    fun submit() {
        val current = mutableState.value
        if (current.isSubmitting) return
        if (current.rating == 0) {
            mutableState.update { it.copy(isRatingMissing = true) }
            return
        }

        val draft = ReviewDraft(rating = current.rating, body = current.body.trim())
        send { repository.save(slug, draft) }
    }

    fun requestDelete() {
        mutableState.update { current -> current.copy(isConfirmingDelete = true) }
    }

    fun dismissDelete() {
        mutableState.update { current -> current.copy(isConfirmingDelete = false) }
    }

    fun confirmDelete() {
        mutableState.update { current -> current.copy(isConfirmingDelete = false) }
        send { repository.delete(slug) }
    }

    private fun send(request: suspend () -> Result<*>) {
        mutableState.update { current -> current.copy(isSubmitting = true, failure = null) }
        viewModelScope.launch {
            val result = request()
            mutableState.update { current ->
                current.copy(
                    isSubmitting = false,
                    isDone = result.isSuccess,
                    failure = result.exceptionOrNull()?.failureReason(),
                )
            }
        }
    }
}

private fun ReviewComposerState.prefilledFrom(own: OwnReview): ReviewComposerState {
    val review = own.review ?: return copy(isLoading = false)

    return copy(
        rating = review.rating,
        body = review.body.orEmpty(),
        isEditing = true,
        isLoading = false,
    )
}
