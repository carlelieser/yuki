package app.yuki.core.model

private const val BYTES_PER_UNIT = 1_000L
private val BYTE_UNITS = listOf("KB", "MB", "GB", "TB")

private const val UNKNOWN_TOTAL = "?"

fun formatByteSize(bytes: Long): String {
    if (bytes < BYTES_PER_UNIT) return "$bytes B"

    var scaled = bytes
    var unitIndex = 0

    while (unitIndex < BYTE_UNITS.lastIndex && scaled >= BYTES_PER_UNIT * BYTES_PER_UNIT) {
        scaled /= BYTES_PER_UNIT
        unitIndex += 1
    }

    return "${oneDecimal(scaled)} ${BYTE_UNITS[unitIndex]}"
}

fun formatByteProgress(bytesDownloaded: Long, bytesTotal: Long?): String {
    val downloaded = formatByteSize(bytesDownloaded)
    val total = bytesTotal?.let(::formatByteSize) ?: UNKNOWN_TOTAL

    return "$downloaded / $total"
}

private fun oneDecimal(scaled: Long): String {
    val tenths = scaled * 10 / BYTES_PER_UNIT
    val whole = tenths / 10
    val fraction = tenths % 10

    return if (fraction == 0L) whole.toString() else "$whole.$fraction"
}
