package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import app.yuki.core.designsystem.component.YukiTextButton
import app.yuki.core.designsystem.theme.YukiSpacing

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
    LabelledField(
        label = NAME_LABEL,
        field = field,
        modifier = modifier.testTag(NAME_FIELD_TAG),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
    )
}

@Composable
internal fun EmailField(field: CredentialField, modifier: Modifier = Modifier) {
    LabelledField(
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
internal fun PasswordField(
    field: CredentialField,
    modifier: Modifier = Modifier,
    onForgotClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        PasswordLabelRow(onForgotClick = onForgotClick)
        FieldBody(
            field = field,
            modifier = Modifier.testTag(PASSWORD_FIELD_TAG),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            isPassword = true,
        )
    }
}

@Composable
private fun PasswordLabelRow(onForgotClick: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldLabel(PASSWORD_LABEL)

        if (onForgotClick != null) {
            YukiTextButton(label = FORGOT_PASSWORD_LABEL, onClick = onForgotClick)
        }
    }
}

@Composable
private fun LabelledField(
    label: String,
    field: CredentialField,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        FieldLabel(label)
        FieldBody(field = field, modifier = modifier, keyboardOptions = keyboardOptions)
    }
}

@Composable
private fun FieldLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun FieldBody(
    field: CredentialField,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = field.value,
        onValueChange = field.onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        isError = field.error != null,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        supportingText = field.error?.let { error -> { Text(text = error) } },
    )
}

internal const val FORGOT_PASSWORD_LABEL = "Forgot password?"
