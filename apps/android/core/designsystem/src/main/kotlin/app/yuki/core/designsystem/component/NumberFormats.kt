package app.yuki.core.designsystem.component

import android.icu.text.CompactDecimalFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private const val TENTHS = 10.0
private const val FRACTION_DIGITS = 1

internal fun formatRating(average: Double, locale: Locale): String {
    val rounded = Math.round(average * TENTHS) / TENTHS
    val format = NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = FRACTION_DIGITS
        maximumFractionDigits = FRACTION_DIGITS
    }

    return format.format(rounded)
}

internal fun formatStarCount(stars: Int, locale: Locale): String {
    val format = CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT).apply {
        setSignificantDigitsUsed(false)
        maximumFractionDigits = FRACTION_DIGITS
        roundingMode = BigDecimal.ROUND_DOWN
    }

    return format.format(stars.toLong())
}

@Composable
internal fun currentLocale(): Locale = LocalConfiguration.current.locales[0]
