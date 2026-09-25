package app.yuki.feature.account

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import app.yuki.core.designsystem.component.YukiTextField

const val EMAIL_FIELD_TAG = "emailField"
const val PASSWORD_FIELD_TAG = "passwordField"
const val NAME_FIELD_TAG = "nameField"

data class CredentialField(
    val value: String,
    val error: CredentialError?,
    val onValueChange: (String) -> Unit,
)

@Composable
internal fun NameField(field: CredentialField, modifier: Modifier = Modifier) {
    CredentialTextField(
        label = stringResource(R.string.account_name_label),
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
        label = stringResource(R.string.account_email_label),
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
        label = stringResource(R.string.account_password_label),
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
    YukiTextField(
        value = field.value,
        onValueChange = field.onValueChange,
        label = label,
        modifier = modifier,
        error = field.error?.text(),
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
    )
}
