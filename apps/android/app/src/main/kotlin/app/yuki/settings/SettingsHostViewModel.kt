package app.yuki.settings

import androidx.lifecycle.ViewModel
import app.yuki.core.settings.api.SettingsContributor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsHostViewModel @Inject constructor(
    val contributors: Set<@JvmSuppressWildcards SettingsContributor>,
) : ViewModel()
