package app.yuki.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSpacing

const val COLLECTION_EMPTY_TAG = "collectionEmpty"

data class EmptyContent(
    val title: String,
    val description: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
private fun EmptyActionButton(content: EmptyContent) {
    val label = content.actionLabel ?: return
    val onAction = content.onAction ?: return

    TextButton(onClick = onAction) { Text(text = label) }
}

@Composable
fun CollectionEmpty(
    content: EmptyContent,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        shape = YukiShape.Card,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag(COLLECTION_EMPTY_TAG),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(YukiSpacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        ) {
            Text(
                text = content.title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = content.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            EmptyActionButton(content = content)
        }
    }
}
