package app.yuki.core.auth

import app.yuki.core.model.AuthAccount

data class AuthSession(
    val token: String,
    val account: AuthAccount,
)
