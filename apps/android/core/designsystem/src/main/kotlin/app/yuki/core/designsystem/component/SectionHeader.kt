package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

enum class SectionHeaderVariant {
    Title,
    Overline,
}

@Composable
private fun sectionHeaderStyle(variant: SectionHeaderVariant): TextStyle = when (variant) {
    SectionHeaderVariant.Title -> MaterialTheme.typography.titleLarge
    SectionHeaderVariant.Overline -> MaterialTheme.typography.labelMedium
}

@Composable
private fun sectionHeaderColor(variant: SectionHeaderVariant) = when (variant) {
    SectionHeaderVariant.Title -> MaterialTheme.colorScheme.onBackground
    SectionHeaderVariant.Overline -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun sectionHeaderText(title: String, variant: SectionHeaderVariant): String =
    when (variant) {
        SectionHeaderVariant.Title -> title
        SectionHeaderVariant.Overline -> title.uppercase()
    }

private fun sectionHeaderHeight(variant: SectionHeaderVariant): Dp = when (variant) {
    SectionHeaderVariant.Title -> YukiSize.SectionHeaderHeight
    SectionHeaderVariant.Overline -> YukiSize.OverlineHeaderHeight
}

private fun sectionHeaderSpacing(variant: SectionHeaderVariant): Dp = when (variant) {
    SectionHeaderVariant.Title -> YukiSpacing.Small
    SectionHeaderVariant.Overline -> YukiSpacing.ExtraSmall
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    variant: SectionHeaderVariant = SectionHeaderVariant.Title,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = sectionHeaderHeight(variant))
            .padding(
                horizontal = YukiSpacing.Large,
                vertical = sectionHeaderSpacing(variant),
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = sectionHeaderText(title, variant),
            style = sectionHeaderStyle(variant),
            color = sectionHeaderColor(variant),
            modifier = Modifier.semantics { heading() },
        )
        action?.invoke()
    }
}
