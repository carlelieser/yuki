package app.yuki.core.installer

import android.content.Intent

internal data class SessionStatus(
    val sessionId: Int,
    val code: Int,
    val message: String?,
    val userAction: Intent?,
)
