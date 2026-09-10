package app.yuki.core.model

private const val THOUSAND = 1_000
private const val MILLION = 1_000_000

fun formatStarCount(stars: Int): String = when {
    stars >= MILLION -> scaled(stars, MILLION, "M")
    stars >= THOUSAND -> scaled(stars, THOUSAND, "k")
    else -> stars.toString()
}

private fun scaled(stars: Int, unit: Int, suffix: String): String {
    val tenths = stars.toLong() * 10 / unit
    val whole = tenths / 10
    val fraction = tenths % 10

    return if (fraction == 0L) "$whole$suffix" else "$whole.$fraction$suffix"
}
