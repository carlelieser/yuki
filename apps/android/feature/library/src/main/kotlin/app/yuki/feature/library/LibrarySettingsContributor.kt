package app.yuki.feature.library

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsGroup as SettingsGroupId
import javax.inject.Inject

const val LIBRARY_CARD_TAG = "libraryCard"
const val SYNC_ROW_TAG = "syncRow"

private const val LIBRARY_ROW_COUNT = 2

internal const val LIBRARY_SECTION_LABEL = "Library"
internal const val VIEW_LIBRARY_TITLE = "View library"
internal const val VIEW_LIBRARY_SUPPORTING = "Browse the apps saved to your account."
internal const val SYNC_TITLE = "Sync"
internal const val SYNC_IDLE_SUPPORTING = "Reconcile installed apps with your library."
internal const val SYNC_RUNNING_SUPPORTING = "Syncing…"

class LibrarySettingsContributor @Inject constructor(
    private val navigation: LibrarySettingsNavigation,
) : SettingsContributor {
    override val group = SettingsGroupId.Library

    @Composable
    override fun Content() {
        val viewModel: LibrarySyncViewModel = hiltViewModel()
        val isSignedIn by viewModel.isSignedIn.collectAsStateWithLifecycle()
        val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

        if (!isSignedIn) return

        SettingsGroup(modifier = Modifier.testTag(LIBRARY_CARD_TAG), label = LIBRARY_SECTION_LABEL) {
            SettingsRow(
                position = SettingsRowPosition(index = 0, count = LIBRARY_ROW_COUNT),
                title = VIEW_LIBRARY_TITLE,
                supporting = VIEW_LIBRARY_SUPPORTING,
                icon = YukiIcons.GridView,
                onClick = navigation.onViewLibraryClick,
            )

            SettingsRow(
                position = SettingsRowPosition(index = 1, count = LIBRARY_ROW_COUNT),
                title = SYNC_TITLE,
                supporting = if (isSyncing) SYNC_RUNNING_SUPPORTING else SYNC_IDLE_SUPPORTING,
                icon = YukiIcons.Sync,
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
    CircularProgressIndicator(modifier = Modifier.size(YukiSize.ProgressCircular))
}
