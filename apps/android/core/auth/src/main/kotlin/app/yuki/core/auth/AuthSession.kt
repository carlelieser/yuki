package app.yuki.core.auth

data class AuthAccount(
    val id: String,
    val name: String,
    val email: String,
    val imageUrl: String?,
)

data class AuthSession(
    val token: String,
    val account: AuthAccount,
)
