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
const val SCREENSHOT_URLS_KEY = "urls"

@HiltViewModel
class ScreenshotViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ListingRepository,
) : ViewModel() {
    private val slug: String = requireNotNull(savedStateHandle[LISTING_SLUG_KEY]) {
        "ScreenshotViewerViewModel requires a '$LISTING_SLUG_KEY' argument"
    }

    val startIndex: Int = savedStateHandle[SCREENSHOT_START_INDEX_KEY] ?: 0

    private val seeded: List<Screenshot> =
        savedStateHandle.get<List<String>>(SCREENSHOT_URLS_KEY)
            .orEmpty()
            .map { url -> Screenshot(url = url, alt = null) }

    private val mutableScreenshots =
        MutableStateFlow<UiState<List<Screenshot>>>(initialState())

    val screenshots: StateFlow<UiState<List<Screenshot>>> = mutableScreenshots.asStateFlow()

    init {
        refresh()
    }

    private fun initialState(): UiState<List<Screenshot>> =
        if (seeded.isEmpty()) UiState.Loading else UiState.Success(seeded)

    fun refresh() {
        mutableScreenshots.value = initialState()
        viewModelScope.launch {
            val detail = repository.detail(slug)
            val refined = detail.map { it.screenshots }.toUiState()

            mutableScreenshots.value = resolve(refined)
        }
    }

    private fun resolve(refined: UiState<List<Screenshot>>): UiState<List<Screenshot>> {
        if (refined is UiState.Success) return refined
        if (seeded.isEmpty()) return refined

        return UiState.Success(seeded)
    }
}
