package app.yuki.core.designsystem.component

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.theme.YukiShape

const val YUKI_SNACKBAR_TAG = "yukiSnackbar"

@Composable
fun YukiSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = YukiShape.Card,
            modifier = Modifier.testTag(YUKI_SNACKBAR_TAG),
        )
    }
}
