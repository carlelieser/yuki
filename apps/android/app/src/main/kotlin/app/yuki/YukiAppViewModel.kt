package app.yuki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.feature.settings.YukiPreferenceReader
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private const val STOP_TIMEOUT_MILLIS = 5_000L
private const val DYNAMIC_COLOR_DEFAULT = true

@HiltViewModel
class YukiAppViewModel @Inject constructor(
    reader: YukiPreferenceReader,
) : ViewModel() {
    val isDynamicColorEnabled: StateFlow<Boolean> = reader.isDynamicColorEnabled()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DYNAMIC_COLOR_DEFAULT,
        )
}
