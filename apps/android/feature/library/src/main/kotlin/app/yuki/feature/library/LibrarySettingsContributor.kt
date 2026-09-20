package app.yuki.feature.library

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsMessage
import app.yuki.core.settings.api.SettingsGroup as SettingsGroupId
import javax.inject.Inject

const val LIBRARY_CARD_TAG = "libraryCard"
const val SYNC_ROW_TAG = "syncRow"

private const val LIBRARY_ROW_COUNT = 2

internal const val LIBRARY_SECTION_LABEL = "Library"
internal const val OPEN_LIBRARY_TITLE = "Open library"
internal const val SYNC_TITLE = "Sync"

class LibrarySettingsContributor @Inject constructor(
    private val navigation: LibrarySettingsNavigation,
) : SettingsContributor {
    override val group = SettingsGroupId.Library

    @Composable
    override fun Content() {
        val viewModel: LibrarySyncViewModel = hiltViewModel()
        val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
        val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
        val message by viewModel.message.collectAsStateWithLifecycle()

        SettingsMessage(message = message, onShown = viewModel::onMessageShown)

        if (!isSignedIn) return

        SettingsGroup(modifier = Modifier.testTag(LIBRARY_CARD_TAG), label = LIBRARY_SECTION_LABEL) {
            SettingsRow(
                position = SettingsRowPosition(index = 0, count = LIBRARY_ROW_COUNT),
                title = OPEN_LIBRARY_TITLE,
                icon = YukiIcons.GridView,
                onClick = navigation.onViewLibraryClick,
            )

            SettingsRow(
                position = SettingsRowPosition(index = 1, count = LIBRARY_ROW_COUNT),
                title = SYNC_TITLE,
                icon = YukiIcons.Sync,
                isEnabled = !isSyncing,
                onClick = viewModel::onSync,
                trailing = if (isSyncing) {
                    { SyncProgress() }
                } else {
                    null
                },
                modifier = Modifier.testTag(SYNC_ROW_TAG),
            )
        }
    }
}

@Composable
private fun SyncProgress() {
    YukiLoadingIndicator(modifier = Modifier.size(YukiSize.ProgressCircular))
}
