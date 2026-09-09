package app.yuki.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.yuki.core.designsystem.R

val GoogleSans = FontFamily(
    Font(R.font.google_sans_regular, FontWeight.Normal),
    Font(R.font.google_sans_medium, FontWeight.Medium),
    Font(R.font.google_sans_semibold, FontWeight.SemiBold),
    Font(R.font.google_sans_bold, FontWeight.Bold),
)

private fun heading(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Bold,
) = TextStyle(
    fontFamily = GoogleSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = (-0.02 * size).sp,
)

private fun body(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = GoogleSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private fun label(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = GoogleSans,
    fontWeight = FontWeight.Medium,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.1.sp,
)

val YukiTypography = Typography(
    displayLarge = heading(size = 57, lineHeight = 64),
    displayMedium = heading(size = 45, lineHeight = 52),
    displaySmall = heading(size = 36, lineHeight = 44),
    headlineLarge = heading(size = 32, lineHeight = 40),
    headlineMedium = heading(size = 28, lineHeight = 36),
    headlineSmall = heading(size = 24, lineHeight = 32),
    titleLarge = heading(size = 22, lineHeight = 28),
    titleMedium = heading(size = 17, lineHeight = 24, weight = FontWeight.SemiBold),
    titleSmall = heading(size = 15, lineHeight = 20, weight = FontWeight.SemiBold),
    bodyLarge = body(size = 16, lineHeight = 24),
    bodyMedium = body(size = 14, lineHeight = 20),
    bodySmall = body(size = 12, lineHeight = 16),
    labelLarge = label(size = 14, lineHeight = 20),
    labelMedium = label(size = 12, lineHeight = 16),
    labelSmall = label(size = 11, lineHeight = 16),
)
