package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import app.yuki.core.designsystem.theme.YukiShape

const val SEARCH_CLEAR_DESCRIPTION = "Clear search"

data class SearchBarState(
    val query: String,
    val placeholder: String = "Search apps",
)

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
) {
    OutlinedTextField(
        value = state.query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = state.placeholder) },
        leadingIcon = { Icon(imageVector = YukiIcons.Search, contentDescription = null) },
        trailingIcon = { ClearAction(query = state.query, onQueryChange = onQueryChange) },
        singleLine = true,
        shape = YukiShape.Pill,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(),
        modifier = modifier.fillMaxWidth(),
    )
}
