package app.yuki.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import app.yuki.core.designsystem.component.LinkOpener

const val LEGAL_NOTICE_TAG = "legalNotice"

internal const val TERMS_LABEL = "Terms of Service"
internal const val PRIVACY_LABEL = "Privacy Policy"

private const val TERMS_PATH = "terms"
private const val PRIVACY_PATH = "privacy"

@Composable
internal fun LegalNotice(
    baseUrl: String,
    onLinkClick: LinkOpener,
    modifier: Modifier = Modifier,
) {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )

    Text(
        text = buildAnnotatedString {
            append("By creating an account, you agree to our ")
            appendLink(TERMS_LABEL, legalUrl(baseUrl, TERMS_PATH), linkStyles, onLinkClick)
            append(" and ")
            appendLink(PRIVACY_LABEL, legalUrl(baseUrl, PRIVACY_PATH), linkStyles, onLinkClick)
            append(".")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .testTag(LEGAL_NOTICE_TAG),
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendLink(
    label: String,
    url: String,
    styles: TextLinkStyles,
    onLinkClick: LinkOpener,
) {
    withLink(
        LinkAnnotation.Clickable(tag = url, styles = styles) { onLinkClick.open(url) },
    ) {
        append(label)
    }
}

internal fun legalUrl(baseUrl: String, path: String): String =
    "${baseUrl.trimEnd('/')}/$path"
