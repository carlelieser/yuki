package app.yuki.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue

class HardcodedComposeTextDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = HardcodedComposeTextDetector()

    override fun getIssues(): List<Issue> = listOf(HardcodedComposeTextDetector.ISSUE)

    private val stubs: Array<TestFile> = arrayOf(
        kotlin(
            """
            package androidx.compose.material3

            fun Text(text: String) {}
            fun Icon(contentDescription: String?) {}
            fun stringResource(id: Int): String = ""
            fun testTag(tag: String) {}
            class Semantics { var contentDescription: String = "" }
            fun semantics(block: Semantics.() -> Unit) {}
            class SnackbarHostState {
                fun showSnackbar(message: String, actionLabel: String? = null) {}
            }
            """,
        ).indented(),
        kotlin(
            """
            package androidx.compose.runtime

            annotation class Composable
            """,
        ).indented(),
        kotlin(
            """
            package app.yuki.core.model

            data class SelfListing(val title: String)
            """,
        ).indented(),
        kotlin(
            """
            package app.yuki.widgets

            import androidx.compose.runtime.Composable

            @Composable
            fun Banner(title: String) {}
            """,
        ).indented(),
        kotlin(
            """
            package androidx.compose.animation.core

            fun animateFloatAsState(target: Float, label: String) {}
            """,
        ).indented(),
    )

    private fun check(source: String) = lint()
        .files(*stubs, kotlin(source).indented())
        .allowMissingSdk()
        .run()

    fun testLiteralTextIsReported() {
        check(
            """
            import androidx.compose.material3.Text

            fun screen() = Text(text = "Library")
            """,
        ).expectErrorCount(1)
    }

    fun testStringTemplateIsReported() {
        check(
            """
            import androidx.compose.material3.Icon

            fun screen(title: String) = Icon(contentDescription = "See all ${'$'}title")
            """,
        ).expectErrorCount(1)
    }

    fun testConstantIsReported() {
        check(
            """
            import androidx.compose.material3.Text

            private const val TITLE = "Library"

            fun screen() = Text(TITLE)
            """,
        ).expectErrorCount(1)
    }

    fun testSemanticsAssignmentIsReported() {
        check(
            """
            import androidx.compose.material3.semantics

            fun screen() = semantics { contentDescription = "Installed" }
            """,
        ).expectErrorCount(1)
    }

    fun testSnackbarMessageIsReported() {
        check(
            """
            import androidx.compose.material3.SnackbarHostState

            fun screen(host: SnackbarHostState) = host.showSnackbar(message = "Saved")
            """,
        ).expectErrorCount(1)
    }

    fun testStringResourceIsClean() {
        check(
            """
            import androidx.compose.material3.Text
            import androidx.compose.material3.stringResource

            fun screen() = Text(text = stringResource(1))
            """,
        ).expectClean()
    }

    fun testDynamicValueIsClean() {
        check(
            """
            import androidx.compose.material3.Text

            fun screen(title: String) = Text(text = title)
            """,
        ).expectClean()
    }

    fun testAnimationLabelIsClean() {
        check(
            """
            import androidx.compose.animation.core.animateFloatAsState

            fun screen() = animateFloatAsState(target = 1f, label = "expandRotation")
            """,
        ).expectClean()
    }

    fun testTagIsClean() {
        check(
            """
            import androidx.compose.material3.testTag

            fun screen() = testTag("libraryList")
            """,
        ).expectClean()
    }

    fun testSymbolsWithoutLettersAreClean() {
        check(
            """
            import androidx.compose.material3.Text

            fun screen() = Text(text = "•")
            """,
        ).expectClean()
    }

    fun testComposableOutsideUiPackagesIsReported() {
        check(
            """
            import app.yuki.widgets.Banner

            fun screen() = Banner(title = "Welcome")
            """,
        ).expectErrorCount(1)
    }

    fun testDomainModelIsClean() {
        check(
            """
            import app.yuki.core.model.SelfListing

            fun listing() = SelfListing(title = "Yuki")
            """,
        ).expectClean()
    }
}
