package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiShape

@Composable
fun YukiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().keyboardClearance(keyboardInsets()),
        label = { Text(text = label) },
        singleLine = true,
        isError = error != null,
        shape = YukiShape.Card,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        supportingText = error?.let { message -> { Text(text = message) } },
        colors = yukiTextFieldColors(),
    )
}

@Composable
private fun yukiTextFieldColors(): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
)

data class TextAreaContent(
    val value: String,
    val placeholder: String,
    val maxLength: Int,
    val minLines: Int = DEFAULT_TEXT_AREA_LINES,
)

private const val DEFAULT_TEXT_AREA_LINES = 4

@Composable
fun YukiTextArea(
    content: TextAreaContent,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    TextField(
        value = content.value,
        onValueChange = { next -> onValueChange(next.take(content.maxLength)) },
        modifier = modifier.fillMaxWidth().keyboardClearance(keyboardInsets()),
        placeholder = { Text(text = content.placeholder) },
        minLines = content.minLines,
        shape = YukiShape.Card,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        supportingText = {
            Text(
                text = stringResource(R.string.designsystem_text_length, content.value.length, content.maxLength),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        colors = yukiTextFieldColors(),
    )
}
