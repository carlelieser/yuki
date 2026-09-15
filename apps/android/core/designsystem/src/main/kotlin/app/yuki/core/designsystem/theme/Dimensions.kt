package app.yuki.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object YukiSpacing {
    val ExtraSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val ExtraLarge = 24.dp
    val Section = 32.dp
}

object YukiSize {
    val IconTiny = 14.dp
    val IconSmall = 24.dp
    val IconMedium = 40.dp
    val IconLarge = 56.dp
    val IconExtraLarge = 72.dp
    val CardWidth = 156.dp
    val ScreenshotWidth = 220.dp
    val MinimumTouchTarget = 48.dp
    val HeaderHeight = 64.dp
    val SectionHeaderHeight = 56.dp
    val OverlineHeaderHeight = 28.dp
    val BadgeHeight = 22.dp
    val BadgeWidth = 64.dp
    val InstalledBadge = 18.dp
    val InstalledBadgeBorder = 1.5.dp
    val IconMediumInProgress = 28.dp
    val TitleLineHeight = 20.dp
    val ProgressCircular = 20.dp
}

object YukiRatio {
    const val Square = 1f
    const val Banner = 16f / 9f
    const val Screenshot = 9f / 16f
}

object YukiShape {
    val Icon = RoundedCornerShape(12.dp)
    val IconLarge = RoundedCornerShape(18.dp)
    val Card = RoundedCornerShape(16.dp)
    val Media = RoundedCornerShape(12.dp)
    val Pill = RoundedCornerShape(28.dp)
}
