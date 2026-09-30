package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val SCREEN_ACTION_TAG = "screenAction"
const val BACK_ACTION_TAG = "backAction"
const val DETAIL_HEADING_ICON_TAG = "detailHeadingIcon"


data class ScreenAction(
    val icon: ImageVector,
    val description: String,
    val onClick: () -> Unit,
)

@Composable
fun YukiScreen(
    title: String,
    modifier: Modifier = Modifier,
    action: ScreenAction? = null,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            YukiScreenHeader(title = title, action = action, trailing = trailing)
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}

@Composable
private fun YukiScreenHeader(
    title: String,
    action: ScreenAction?,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(YukiSize.HeaderHeight)
            .padding(horizontal = YukiSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        ScreenActionButton(action = action)
        trailing?.invoke()
    }
}

@Composable
private fun ScreenActionButton(action: ScreenAction?) {
    if (action == null) return

    IconButton(
        onClick = action.onClick,
        modifier = Modifier.testTag(SCREEN_ACTION_TAG),
    ) {
        Icon(imageVector = action.icon, contentDescription = action.description)
    }
}

data class DetailHeading(
    val title: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val hasIcon: Boolean = iconUrl != null,
)

@Composable
fun YukiDetailScreen(
    title: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    YukiDetailScreen(
        heading = DetailHeading(title = title),
        onBackClick = onBackClick,
        modifier = modifier,
        trailing = trailing,
        content = content,
    )
}

@Composable
fun YukiDetailScreen(
    heading: DetailHeading,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            YukiDetailHeader(heading = heading, onBackClick = onBackClick, trailing = trailing)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars),
            ) {
                content()
            }
        }
    }
}

@Composable
private fun YukiDetailHeader(
    heading: DetailHeading,
    onBackClick: () -> Unit,
    trailing: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(YukiSize.HeaderHeight)
            .padding(end = YukiSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag(BACK_ACTION_TAG),
        ) {
            Icon(imageVector = YukiIcons.Back, contentDescription = stringResource(R.string.designsystem_back))
        }
        DetailHeadingText(heading = heading, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
private fun DetailHeadingText(heading: DetailHeading, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (heading.hasIcon) {
            AppIcon(
                iconUrl = heading.iconUrl,
                size = YukiSize.IconMedium,
                modifier = Modifier.testTag(DETAIL_HEADING_ICON_TAG),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = heading.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            ProductDescription(description = heading.description, maxLines = 1)
        }
    }
}

@Composable
fun YukiScreenCenter(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
