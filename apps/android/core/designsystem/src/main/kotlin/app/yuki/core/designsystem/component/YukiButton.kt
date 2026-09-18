package app.yuki.core.designsystem.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize

@Composable
private fun ButtonLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

private fun Modifier.buttonSize(): Modifier =
    animateContentSize(animationSpec = YukiMotion.resize())
        .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)

@Composable
fun YukiButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled,
        shape = YukiShape.Pill,
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier.buttonSize(),
    ) {
        ButtonLabel(text = label)
    }
}

@Composable
fun YukiSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = isEnabled,
        shape = YukiShape.Pill,
        contentPadding = ButtonDefaults.ContentPadding,
        modifier = modifier.buttonSize(),
    ) {
        ButtonLabel(text = label)
    }
}

@Composable
fun YukiTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        enabled = isEnabled,
        shape = YukiShape.Pill,
        modifier = modifier.buttonSize(),
    ) {
        ButtonLabel(text = label)
    }
}
