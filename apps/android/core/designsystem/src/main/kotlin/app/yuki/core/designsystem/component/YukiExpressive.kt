package app.yuki.core.designsystem.component

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YukiLoadingIndicator(modifier: Modifier = Modifier) {
    LoadingIndicator(modifier = modifier)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YukiContainedLoadingIndicator(modifier: Modifier = Modifier) {
    ContainedLoadingIndicator(modifier = modifier)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YukiNavBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ShortNavigationBar(modifier = modifier, content = content)
}

private const val MAX_BADGE_COUNT = 99

data class YukiNavDestination(
    val label: String,
    val icon: @Composable () -> Unit,
    val badgeCount: Int = 0,
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YukiNavBarItem(
    destination: YukiNavDestination,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    ShortNavigationBarItem(
        selected = isSelected,
        onClick = onSelect,
        icon = { BadgedNavIcon(destination) },
        label = { Text(text = destination.label) },
    )
}

@Composable
private fun BadgedNavIcon(destination: YukiNavDestination) {
    if (destination.badgeCount <= 0) {
        destination.icon()
        return
    }

    BadgedBox(
        badge = {
            Badge(modifier = Modifier.testTag(NAV_BADGE_TAG).clearAndSetSemantics {}) {
                Text(text = badgeText(destination.badgeCount))
            }
        },
        content = { destination.icon() },
    )
}

private fun badgeText(count: Int): String =
    if (count > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else count.toString()

const val NAV_BADGE_TAG = "navBadge"
