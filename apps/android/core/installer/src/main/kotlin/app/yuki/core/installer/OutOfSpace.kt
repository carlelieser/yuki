package app.yuki.core.installer

private const val OUT_OF_SPACE_ERRNO = "ENOSPC"

internal fun Throwable.isOutOfSpace(): Boolean =
    generateSequence(this, Throwable::cause).any { cause ->
        cause.message.orEmpty().contains(OUT_OF_SPACE_ERRNO)
    }
