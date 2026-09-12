package app.yuki.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.model.Screenshot
import app.yuki.core.model.UiState
import app.yuki.core.model.toUiState
import app.yuki.core.network.ListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

const val SCREENSHOT_START_INDEX_KEY = "startIndex"

@HiltViewModel
class ScreenshotViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ScreenshotViewerViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    val startIndex: Int = savedStateHandle[SCREENSHOT_START_INDEX_KEY] ?: 0

    private val mutableScreenshots =
        MutableStateFlow<UiState<List<Screenshot>>>(UiState.Loading)

    val screenshots: StateFlow<UiState<List<Screenshot>>> = mutableScreenshots.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        mutableScreenshots.value = UiState.Loading
        viewModelScope.launch {
            val detail = repository.detail(slug)
            mutableScreenshots.value = detail.map { it.screenshots }.toUiState()
        }
    }
}
