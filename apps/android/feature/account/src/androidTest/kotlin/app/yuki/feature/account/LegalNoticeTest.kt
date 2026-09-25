package app.yuki.feature.account

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.semantics.SemanticsProperties
import app.yuki.core.designsystem.component.LinkOpener
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LegalNoticeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(id: Int): String = composeRule.activity.getString(id)

    @Test
    fun theTermsAndPrivacyLabelsLinkToTheirPages() {
        composeRule.setContent {
            LegalNotice(baseUrl = "https://yukistore.org/", onLinkClick = LinkOpener { })
        }

        val rendered = composeRule.onNodeWithTag(LEGAL_NOTICE_TAG)
            .fetchSemanticsNode()
            .config[SemanticsProperties.Text]
            .single()

        assertEquals(
            mapOf(
                text(R.string.account_legal_terms) to "https://yukistore.org/terms",
                text(R.string.account_legal_privacy) to "https://yukistore.org/privacy",
            ),
            rendered.linkTargets(),
        )
    }
}

private fun AnnotatedString.linkTargets(): Map<String, String> =
    getLinkAnnotations(0, length).associate { range ->
        substring(range.start, range.end) to (range.item as LinkAnnotation.Clickable).tag
    }
