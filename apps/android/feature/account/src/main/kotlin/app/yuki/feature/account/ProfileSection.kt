package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.yuki.core.designsystem.component.Avatar
import app.yuki.core.designsystem.component.AvatarContent
import app.yuki.core.designsystem.component.AvatarSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.AuthAccount

@Composable
internal fun ProfileSection(
    account: AuthAccount,
    onEditAvatarClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Medium, Alignment.CenterVertically),
    ) {
        Avatar(
            content = AvatarContent(imageUrl = account.imageUrl, displayName = account.name),
            size = AvatarSize.Large,
            onEditClick = onEditAvatarClick,
        )

        Text(
            text = account.name,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = account.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
