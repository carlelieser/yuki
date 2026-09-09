package app.yuki.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent
import app.yuki.core.designsystem.component.StatusAction
import app.yuki.core.designsystem.component.StatusCard
import app.yuki.core.designsystem.component.StatusContent
import app.yuki.core.designsystem.theme.YukiSpacing

const val SHIZUKU_CARD_TAG = "shizukuCard"

internal fun LazyListScope.shizukuSection(
    content: SettingsContent,
    onShizukuAction: (ShizukuActionKind) -> Unit,
) {
    item { SectionHeader(title = SHIZUKU_SECTION_TITLE) }
    item { ShizukuStatus(card = content.card, onShizukuAction = onShizukuAction) }

    item {
        SettingsItem(
            content = SettingsItemContent(
                title = SHIZUKU_MODE_TITLE,
                supporting = content.modeLabel,
            ),
        )
    }

    item {
        SettingsItem(
            content = SettingsItemContent(
                title = SHIZUKU_API_TITLE,
                supporting = content.apiVersionLabel,
            ),
        )
    }
}

@Composable
private fun ShizukuStatus(card: ShizukuCard, onShizukuAction: (ShizukuActionKind) -> Unit) {
    StatusCard(
        content = StatusContent(
            title = card.title,
            description = card.description,
            tone = card.tone,
            action = statusActionFor(card, onShizukuAction),
        ),
        modifier = Modifier
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Small)
            .testTag(SHIZUKU_CARD_TAG),
    )
}

private fun statusActionFor(
    card: ShizukuCard,
    onShizukuAction: (ShizukuActionKind) -> Unit,
): StatusAction? {
    val label = card.actionLabel ?: return null
    val kind = card.actionKind ?: return null

    return StatusAction(label = label, onClick = { onShizukuAction(kind) })
}

internal const val SHIZUKU_SECTION_TITLE = "Shizuku"
internal const val SHIZUKU_MODE_TITLE = "Privilege mode"
internal const val SHIZUKU_API_TITLE = "Shizuku API version"
