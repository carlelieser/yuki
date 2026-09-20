package app.yuki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.feature.settings.AppearanceMode
import app.yuki.feature.settings.YukiPreferenceReader
import app.yuki.settings.SettingsNavigationHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class ThemeSettings(
    val appearance: AppearanceMode,
    val isDynamicColorEnabled: Boolean,
)

@HiltViewModel
class YukiAppViewModel @Inject constructor(
    reader: YukiPreferenceReader,
    val settingsNavigation: SettingsNavigationHolder,
) : ViewModel() {
    val theme: StateFlow<ThemeSettings?> =
        combine(reader.appearance(), reader.isDynamicColorEnabled(), ::ThemeSettings)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )
}
