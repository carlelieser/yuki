package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import app.yuki.core.designsystem.theme.YukiShape

const val SEARCH_CLEAR_DESCRIPTION = "Clear search"

data class SearchBarState(
    val query: String,
    val placeholder: String = "Search apps",
)

@Composable
private fun TrailingActions(
    state: SearchBarState,
    onQueryChange: (String) -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ClearAction(query = state.query, onQueryChange = onQueryChange)
        trailing?.invoke()
    }
}

@Composable
private fun ClearAction(query: String, onQueryChange: (String) -> Unit) {
    if (query.isEmpty()) return

    IconButton(onClick = { onQueryChange("") }) {
        Icon(imageVector = YukiIcons.Close, contentDescription = SEARCH_CLEAR_DESCRIPTION)
    }
}

@Composable
fun SearchBar(
    state: SearchBarState,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = state.placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = YukiIcons.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            TrailingActions(
                state = state,
                onQueryChange = onQueryChange,
                trailing = trailing,
            )
        },
        singleLine = true,
        shape = YukiShape.Pill,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
