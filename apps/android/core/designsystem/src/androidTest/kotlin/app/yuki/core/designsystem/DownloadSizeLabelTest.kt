package app.yuki.core.designsystem

import android.text.format.Formatter
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import app.yuki.core.designsystem.component.label
import app.yuki.core.model.DownloadSize
import org.junit.Rule
import org.junit.Test

class DownloadSizeLabelTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun render(size: DownloadSize) {
        composeRule.setContent { Text(text = size.label()) }
    }

    private fun bytes(count: Long): String = Formatter.formatShortFileSize(composeRule.activity, count)

    @Test
    fun aKnownTotalReadsAsDownloadedOverTotal() {
        render(DownloadSize(bytesDownloaded = 4_100_000L, bytesTotal = 12_100_000L))

        composeRule.onNodeWithText("${bytes(4_100_000L)} / ${bytes(12_100_000L)}").assertIsDisplayed()
    }

    @Test
    fun anUnknownTotalReadsAsAQuestionMark() {
        render(DownloadSize(bytesDownloaded = 4_100_000L, bytesTotal = null))

        composeRule.onNodeWithText("${bytes(4_100_000L)} / ?").assertIsDisplayed()
    }
}
