package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import app.yuki.core.designsystem.component.RatingAverage
import app.yuki.core.designsystem.component.RatingStars
import app.yuki.core.designsystem.component.RatingStarsSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.RatingBucket
import app.yuki.core.model.RatingSummary

const val RATING_SUMMARY_TAG = "ratingSummary"

@Composable
internal fun RatingSummaryCard(summary: RatingSummary, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.Large)
            .testTag(RATING_SUMMARY_TAG),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraLarge),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        ) {
            RatingAverage(value = summary.average, style = MaterialTheme.typography.displayMedium)
            RatingStars(value = summary.average, size = RatingStarsSize.Large)
            Text(
                text = pluralStringResource(R.plurals.listing_reviews_count, summary.total, summary.total),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        ) {
            summary.distribution.forEach { bucket ->
                DistributionRow(bucket = bucket, total = summary.total)
            }
        }
    }
}

@Composable
private fun DistributionRow(bucket: RatingBucket, total: Int) {
    val share = if (total == 0) 0f else bucket.count.toFloat() / total

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
    ) {
        Text(text = bucket.rating.toString(), style = MaterialTheme.typography.labelMedium)
        LinearProgressIndicator(
            progress = { share },
            modifier = Modifier.weight(1f),
            drawStopIndicator = {},
        )
        Text(
            text = bucket.count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
