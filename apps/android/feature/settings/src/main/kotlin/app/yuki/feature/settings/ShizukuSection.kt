package app.yuki.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.SettingsSlotRow
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiTextButton
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val SHIZUKU_CARD_TAG = "shizukuCard"

private const val SHIZUKU_ROW_COUNT = 3

@Composable
internal fun ShizukuSection(
    content: SettingsContent,
    onShizukuAction: (ShizukuActionKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(modifier = modifier, label = SHIZUKU_SECTION_TITLE) {
        ShizukuStatusRow(card = content.card, onShizukuAction = onShizukuAction)

        SettingsRow(
            position = SettingsRowPosition(index = 1, count = SHIZUKU_ROW_COUNT),
            title = SHIZUKU_MODE_TITLE,
            supporting = content.modeLabel,
        )

        SettingsRow(
            position = SettingsRowPosition(index = 2, count = SHIZUKU_ROW_COUNT),
            title = SHIZUKU_API_TITLE,
            supporting = content.apiVersionLabel,
        )
    }
}

@Composable
private fun ShizukuStatusRow(card: ShizukuCard, onShizukuAction: (ShizukuActionKind) -> Unit) {
    SettingsSlotRow(
        position = SettingsRowPosition(index = 0, count = SHIZUKU_ROW_COUNT),
        modifier = Modifier.testTag(SHIZUKU_CARD_TAG),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = YukiSpacing.Small),
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    imageVector = statusIconFor(card.tone),
                    contentDescription = null,
                    modifier = Modifier.size(YukiSize.IconSmall),
                )

                Column(verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall)) {
                    Text(text = card.title, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = card.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            ShizukuAction(card = card, onShizukuAction = onShizukuAction)
        }
    }
}

@Composable
private fun ShizukuAction(card: ShizukuCard, onShizukuAction: (ShizukuActionKind) -> Unit) {
    val label = card.actionLabel ?: return
    val kind = card.actionKind ?: return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        YukiTextButton(label = label, onClick = { onShizukuAction(kind) })
    }
}

@Composable
private fun statusIconFor(tone: StatusTone): ImageVector = when (tone) {
    StatusTone.Positive -> YukiIcons.Check
    StatusTone.Attention -> YukiIcons.Error
    StatusTone.Informative, StatusTone.Neutral -> YukiIcons.Info
}

internal const val SHIZUKU_SECTION_TITLE = "Shizuku"
internal const val SHIZUKU_MODE_TITLE = "Privilege mode"
internal const val SHIZUKU_API_TITLE = "Version"
