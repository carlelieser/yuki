package app.yuki.core.model

data class LinkedAccount(
    val id: String,
    val providerId: String,
    val username: String?,
    val profileUrl: String?,
)
