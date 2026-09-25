package app.yuki.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.YukiSnackbarHost
import app.yuki.core.settings.api.SettingsMessage
import org.junit.Rule
import org.junit.Test

class SettingsMessageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aSettingsMessageShowsInTheSharedSnackbarHost() {
        val hostState = SnackbarHostState()

        composeRule.setContent {
            CompositionLocalProvider(LocalYukiSnackbarHostState provides hostState) {
                Box {
                    SettingsMessage(message = MESSAGE, onShown = {})
                    YukiSnackbarHost(hostState = hostState)
                }
            }
        }

        composeRule.onNodeWithText(MESSAGE).assertIsDisplayed()
    }
}

private const val MESSAGE = "Library synced"
