package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.AccountAvatar
import app.yuki.core.designsystem.component.AvatarContent
import app.yuki.core.designsystem.component.AvatarSize
import app.yuki.core.designsystem.component.RatingStars
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.Review
import java.time.ZoneId

const val REVIEW_CARD_TAG = "reviewCard"

@Composable
internal fun ReviewCard(
    review: Review,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.Large)
            .testTag(REVIEW_CARD_TAG),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
    ) {
        ReviewByline(review = review, onEdit = onEdit)

        val body = review.body ?: return@Column
        Text(text = body, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ReviewByline(review: Review, onEdit: (() -> Unit)?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        AccountAvatar(
            content = AvatarContent(imageUrl = review.author.imageUrl, displayName = review.author.name),
            size = AvatarSize.Small,
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        ) {
            Text(text = review.author.name, style = MaterialTheme.typography.titleSmall)
            ReviewMeta(review = review)
        }

        if (onEdit == null) return@Row
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = YukiIcons.Edit,
                contentDescription = stringResource(R.string.listing_reviews_edit),
            )
        }
    }
}

@Composable
private fun ReviewMeta(review: Review) {
    val locale = LocalConfiguration.current.locales[0]
    val date = review.createdAt?.let { instant ->
        formatPublished(instant, locale, ZoneId.systemDefault())
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
    ) {
        RatingStars(value = review.rating.toDouble())

        if (date == null) return@Row
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
