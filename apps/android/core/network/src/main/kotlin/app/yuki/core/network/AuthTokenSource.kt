package app.yuki.core.network

fun interface AuthTokenSource {
    suspend fun token(): String?
}
