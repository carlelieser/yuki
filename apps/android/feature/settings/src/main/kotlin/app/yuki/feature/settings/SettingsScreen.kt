package app.yuki.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsGroup

const val SETTINGS_LIST_TAG = "settingsList"

@Composable
fun SettingsScreen(
    contributors: Set<SettingsContributor>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val sections = remember(contributors) { contributors.ordered().partitionFooter() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .heightIn(min = maxHeight)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding)
                .padding(YukiSpacing.Large),
        ) {
            Column(
                modifier = Modifier.testTag(SETTINGS_LIST_TAG),
                verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
            ) {
                sections.body.forEach { contributor -> contributor.Content() }
            }

            Box(
                modifier = Modifier
                    .heightIn(min = YukiSpacing.Section)
                    .weight(1f),
            )

            sections.footer.forEach { contributor -> contributor.Content() }
        }
    }
}

internal data class SettingsSections(
    val body: List<SettingsContributor>,
    val footer: List<SettingsContributor>,
)

internal fun List<SettingsContributor>.partitionFooter(): SettingsSections {
    val (footer, body) = partition { contributor -> contributor.group == SettingsGroup.Legal }

    return SettingsSections(body = body, footer = footer)
}

internal fun Set<SettingsContributor>.ordered(): List<SettingsContributor> =
    sortedWith(compareBy({ contributor -> contributor.group.ordinal }, SettingsContributor::order))
