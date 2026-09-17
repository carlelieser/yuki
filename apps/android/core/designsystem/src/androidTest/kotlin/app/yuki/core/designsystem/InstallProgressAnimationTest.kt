package app.yuki.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.yuki.core.designsystem.component.INSTALL_PROGRESS_TAG
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.theme.YukiTheme
import app.yuki.core.model.InstallState
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TOTAL_BYTES = 1000L
private const val SETTLE_MILLIS = 2_000L
private const val MID_ANIMATION_MILLIS = 48L

@RunWith(AndroidJUnit4::class)
class InstallProgressAnimationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var downloaded by mutableStateOf(0L)

    private fun render() {
        composeRule.setContent {
            YukiTheme(isDynamicColorEnabled = false) {
                InstallButton(
                    state = InstallState.Downloading(downloadSizeOf(downloaded, TOTAL_BYTES)),
                    onAction = InstallActionHandler { },
                )
            }
        }
        composeRule.mainClock.autoAdvance = false
        settle()
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun rangeInfo(): ProgressBarRangeInfo {
        val root = composeRule
            .onNodeWithTag(INSTALL_PROGRESS_TAG, useUnmergedTree = true)
            .fetchSemanticsNode()

        return requireNotNull(findRangeInfo(root)) {
            "No progress semantics under the '$INSTALL_PROGRESS_TAG' node"
        }
    }

    private fun findRangeInfo(node: SemanticsNode): ProgressBarRangeInfo? {
        val own = node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)
        if (own != null) return own

        return node.children.firstNotNullOfOrNull(::findRangeInfo)
    }

    private fun reportedProgress(): Float = rangeInfo().current

    @Test
    fun progressPassesThroughIntermediateValuesInsteadOfJumping() {
        render()

        composeRule.runOnUiThread { downloaded = TOTAL_BYTES }
        composeRule.mainClock.advanceTimeBy(MID_ANIMATION_MILLIS)

        val midpoint = reportedProgress()
        assertTrue(
            "Progress should animate toward its target, was $midpoint",
            midpoint > 0f && midpoint < 1f,
        )
    }

    @Test
    fun progressStillReachesItsTarget() {
        render()

        composeRule.runOnUiThread { downloaded = TOTAL_BYTES }
        settle()

        val settled = reportedProgress()
        assertTrue("Progress should settle at its target, was $settled", settled > 0.99f)
    }

    @Test
    fun progressReportsARangeWhileDownloading() {
        render()

        assertTrue(rangeInfo() != ProgressBarRangeInfo.Indeterminate)
    }
}
