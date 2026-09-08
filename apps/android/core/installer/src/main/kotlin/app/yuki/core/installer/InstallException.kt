package app.yuki.core.installer

import app.yuki.core.model.InstallFailure

class InstallException(
    val failure: InstallFailure,
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
