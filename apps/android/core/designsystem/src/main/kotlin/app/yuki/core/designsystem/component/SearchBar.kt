package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiShape


data class SearchBarState(
    val query: String,
    val placeholder: String? = null,
)

data class SearchBarFocus(
    val onFocusChange: (Boolean) -> Unit,
)

@Composable
private fun ClearAction(query: String, onQueryChange: (String) -> Unit) {
    if (query.isEmpty()) return

    IconButton(onClick = { onQueryChange("") }) {
        Icon(imageVector = YukiIcons.Close, contentDescription = stringResource(R.string.designsystem_search_clear))
    }
}

@Composable
fun SearchBar(
    state: SearchBarState,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focus: SearchBarFocus? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    TextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                text = state.placeholder ?: stringResource(R.string.designsystem_search_placeholder),
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
        trailingIcon = { ClearAction(query = state.query, onQueryChange = onQueryChange) },
        singleLine = true,
        shape = YukiShape.Pill,
        textStyle = MaterialTheme.typography.bodyLarge,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = modifier
            .fillMaxWidth()
            .searchBarFocus(focus),
    )
}

private fun Modifier.searchBarFocus(focus: SearchBarFocus?): Modifier {
    if (focus == null) return this

    return onFocusChanged { state -> focus.onFocusChange(state.isFocused) }
}
