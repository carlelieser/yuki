package app.yuki.core.settings.api

import androidx.compose.runtime.Composable

enum class SettingsGroup {
    Account,
    Library,
    System,
    Legal,
}

interface SettingsContributor {
    val group: SettingsGroup

    val order: Int get() = 0

    @Composable
    fun Content()
}
