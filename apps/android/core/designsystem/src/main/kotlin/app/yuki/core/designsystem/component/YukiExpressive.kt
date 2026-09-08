package app.yuki.core.designsystem.component

import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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

data class YukiNavDestination(
    val label: String,
    val icon: @Composable () -> Unit,
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
        icon = destination.icon,
        label = { Text(text = destination.label) },
    )
}
