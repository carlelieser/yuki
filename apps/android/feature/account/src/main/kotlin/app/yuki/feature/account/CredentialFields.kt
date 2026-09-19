package app.yuki.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation

const val EMAIL_FIELD_TAG = "emailField"
const val PASSWORD_FIELD_TAG = "passwordField"
const val NAME_FIELD_TAG = "nameField"

internal const val EMAIL_LABEL = "Email"
internal const val PASSWORD_LABEL = "Password"
internal const val NAME_LABEL = "Name"

data class CredentialField(
    val value: String,
    val error: String?,
    val onValueChange: (String) -> Unit,
)

@Composable
internal fun NameField(field: CredentialField, modifier: Modifier = Modifier) {
    CredentialTextField(
        label = NAME_LABEL,
        field = field,
        modifier = modifier.testTag(NAME_FIELD_TAG),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
internal fun EmailField(field: CredentialField, modifier: Modifier = Modifier) {
    CredentialTextField(
        label = EMAIL_LABEL,
        field = field,
        modifier = modifier.testTag(EMAIL_FIELD_TAG),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
    )
}

@Composable
internal fun PasswordField(field: CredentialField, modifier: Modifier = Modifier) {
    CredentialTextField(
        label = PASSWORD_LABEL,
        field = field,
        modifier = modifier.testTag(PASSWORD_FIELD_TAG),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        visualTransformation = PasswordVisualTransformation(),
    )
}

@Composable
private fun CredentialTextField(
    label: String,
    field: CredentialField,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = field.value,
        onValueChange = field.onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(text = label) },
        singleLine = true,
        isError = field.error != null,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        supportingText = field.error?.let { error -> { Text(text = error) } },
    )
}
