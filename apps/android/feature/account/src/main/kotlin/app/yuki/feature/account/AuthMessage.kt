package app.yuki.feature.account

data class AuthMessageAction(
    val label: String,
    val onAction: () -> Unit,
)

data class AuthMessage(
    val text: String,
    val onShown: () -> Unit,
    val action: AuthMessageAction? = null,
)
