package app.yuki.core.model

private const val TENTHS = 10

fun formatRating(average: Double): String {
    val tenths = Math.round(average * TENTHS)

    return "${tenths / TENTHS}.${tenths % TENTHS}"
}
