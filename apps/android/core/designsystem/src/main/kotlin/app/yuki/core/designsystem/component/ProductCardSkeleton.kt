package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.theme.YukiRatio
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val LOADING_PLACEHOLDER_DESCRIPTION = "Loading"

@Composable
fun ProductCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        shape = YukiShape.Card,
        modifier = modifier
            .width(YukiSize.CardWidth)
            .semantics { contentDescription = LOADING_PLACEHOLDER_DESCRIPTION },
    ) {
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(YukiRatio.Square),
            shape = YukiShape.Card,
        )
        Column(
            modifier = Modifier.padding(YukiSpacing.Medium),
            verticalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        ) {
            ShimmerBox(modifier = Modifier.fillMaxWidth(fraction = 0.75f).height(14.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(fraction = 0.5f).height(12.dp))
        }
    }
}
