package app.yuki.feature.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSpacing

const val AUTH_MESSAGE_TAG = "authMessage"

@Composable
internal fun AuthMessage(title: String, message: String?) {
    if (message == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer, YukiShape.Card)
            .padding(YukiSpacing.Large)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag(AUTH_MESSAGE_TAG),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}
