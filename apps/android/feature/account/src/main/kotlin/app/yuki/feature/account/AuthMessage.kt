package app.yuki.feature.account

data class AuthMessage(
    val text: String,
    val onShown: () -> Unit,
)
