package app.yuki.core.designsystem.component

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

private val KEYBOARD_CLEARANCE = 48.dp

@Composable
internal fun keyboardClearance(): Modifier {
    val requester = remember { BringIntoViewRequester() }
    var isFocused by remember { mutableStateOf(false) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    val keyboard = keyboardInsets()
    val density = LocalDensity.current

    LaunchedEffect(isFocused) {
        if (!isFocused) return@LaunchedEffect

        val clearance = with(density) { KEYBOARD_CLEARANCE.toPx() }
        snapshotFlow { keyboard.getBottom(density) }.collectLatest {
            requester.bringIntoView(Rect(0f, 0f, size.width.toFloat(), size.height + clearance))
        }
    }

    return Modifier
        .bringIntoViewRequester(requester)
        .onSizeChanged { measured -> size = measured }
        .onFocusChanged { state -> isFocused = state.hasFocus }
}
