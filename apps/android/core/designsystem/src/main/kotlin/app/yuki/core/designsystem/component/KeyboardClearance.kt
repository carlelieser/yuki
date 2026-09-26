package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.LayoutAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private val KEYBOARD_CLEARANCE = 48.dp

internal fun Modifier.keyboardClearance(keyboard: WindowInsets): Modifier =
    this then KeyboardClearanceElement(keyboard)

private data class KeyboardClearanceElement(
    val keyboard: WindowInsets,
) : ModifierNodeElement<KeyboardClearanceNode>() {
    override fun create(): KeyboardClearanceNode = KeyboardClearanceNode(keyboard)

    override fun update(node: KeyboardClearanceNode) {
        node.keyboard = keyboard
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "keyboardClearance"
    }
}

private class KeyboardClearanceNode(var keyboard: WindowInsets) :
    Modifier.Node(),
    FocusEventModifierNode,
    LayoutAwareModifierNode,
    CompositionLocalConsumerModifierNode {
    private var size = IntSize.Zero
    private var isFocused = false
    private var following: Job? = null

    override fun onRemeasured(size: IntSize) {
        this.size = size
    }

    override fun onFocusEvent(focusState: FocusState) {
        if (focusState.hasFocus == isFocused) return

        isFocused = focusState.hasFocus
        following?.cancel()
        following = if (isFocused) coroutineScope.launch { followKeyboard() } else null
    }

    private suspend fun followKeyboard() {
        val density = currentValueOf(LocalDensity)
        val clearance = with(density) { KEYBOARD_CLEARANCE.toPx() }

        snapshotFlow { keyboard.getBottom(density) }.collectLatest {
            bringIntoView { Rect(0f, 0f, size.width.toFloat(), size.height + clearance) }
        }
    }
}
