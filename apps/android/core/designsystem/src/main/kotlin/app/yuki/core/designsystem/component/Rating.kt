package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.MAX_REVIEW_RATING
import app.yuki.core.model.MIN_REVIEW_RATING
import kotlin.math.roundToInt

const val RATING_INPUT_TAG = "ratingInput"

private const val UNFILLED_STAR_ALPHA = 0.38f

enum class RatingStarsSize(val icon: Dp) {
    Small(YukiSize.IconTiny),
    Large(YukiSize.IconSmall),
}

private val STAR_VALUES = MIN_REVIEW_RATING..MAX_REVIEW_RATING

@Composable
private fun starColor(isFilled: Boolean): Color {
    val color = MaterialTheme.colorScheme.primary
    if (isFilled) return color

    return MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = UNFILLED_STAR_ALPHA)
}

@Composable
fun RatingStars(
    value: Double,
    modifier: Modifier = Modifier,
    size: RatingStarsSize = RatingStarsSize.Small,
) {
    val filled = value.roundToInt()
    val description = stringResource(
        R.string.designsystem_badge_rating,
        formatRating(value, currentLocale()),
    )

    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        STAR_VALUES.forEach { star ->
            Icon(
                imageVector = YukiIcons.Star,
                contentDescription = null,
                tint = starColor(isFilled = star <= filled),
                modifier = Modifier.size(size.icon),
            )
        }
    }
}

@Composable
fun RatingInput(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.selectableGroup().testTag(RATING_INPUT_TAG),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        STAR_VALUES.forEach { star ->
            RatingInputStar(star = star, selected = value, onSelect = onValueChange)
        }
    }
}

@Composable
private fun RatingInputStar(star: Int, selected: Int, onSelect: (Int) -> Unit) {
    val description = pluralStringResource(R.plurals.designsystem_rating_stars, star, star)

    Icon(
        imageVector = YukiIcons.Star,
        contentDescription = description,
        tint = starColor(isFilled = star <= selected),
        modifier = Modifier
            .size(YukiSize.MinimumTouchTarget)
            .selectable(
                selected = star == selected,
                role = Role.RadioButton,
                onClick = { onSelect(star) },
            ),
    )
}

@Composable
fun RatingAverage(value: Double, style: TextStyle, modifier: Modifier = Modifier) {
    Text(text = formatRating(value, currentLocale()), style = style, modifier = modifier)
}
