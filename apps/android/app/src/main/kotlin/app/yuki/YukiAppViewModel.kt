package app.yuki

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.installer.InstallProgress
import app.yuki.feature.settings.AppearanceMode
import app.yuki.feature.settings.YukiPreferenceReader
import app.yuki.install.InstallFailureNotices
import app.yuki.settings.SettingsNavigationHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ThemeSettings(
    val appearance: AppearanceMode,
    val isDynamicColorEnabled: Boolean,
)

@HiltViewModel
class YukiAppViewModel @Inject internal constructor(
    reader: YukiPreferenceReader,
    val settingsNavigation: SettingsNavigationHolder,
    private val installFailures: InstallFailureNotices,
) : ViewModel() {
    val theme: StateFlow<ThemeSettings?> =
        combine(reader.appearance(), reader.isDynamicColorEnabled(), ::ThemeSettings)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    val installFailure: StateFlow<InstallProgress?> = installFailures.oldestFailure()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = null,
        )

    fun onInstallRetry(failed: InstallProgress) {
        viewModelScope.launch { installFailures.retry(failed) }
    }

    fun onInstallFailureDismissed(failed: InstallProgress) {
        viewModelScope.launch { installFailures.dismiss(failed) }
    }
}
