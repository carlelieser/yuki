package app.yuki.feature.account

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import app.yuki.core.designsystem.component.LinkOpener

const val LEGAL_NOTICE_TAG = "legalNotice"

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

    val terms = stringResource(R.string.account_legal_terms)
    val privacy = stringResource(R.string.account_legal_privacy)
    val sentence = stringResource(R.string.account_legal_notice, terms, privacy)

    val links = listOf(
        LegalLink(sentence, terms, legalUrl(baseUrl, TERMS_PATH)),
        LegalLink(sentence, privacy, legalUrl(baseUrl, PRIVACY_PATH)),
    )

    Text(
        text = buildAnnotatedString {
            append(sentence)
            links.forEach { link -> addLegalLink(link, linkStyles, onLinkClick) }
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .testTag(LEGAL_NOTICE_TAG),
    )
}

private data class LegalLink(val sentence: String, val label: String, val url: String)

private fun AnnotatedString.Builder.addLegalLink(
    link: LegalLink,
    styles: TextLinkStyles,
    onLinkClick: LinkOpener,
) {
    val start = link.sentence.indexOf(link.label)
    if (start < 0) return

    addLink(
        LinkAnnotation.Clickable(tag = link.url, styles = styles) { onLinkClick.open(link.url) },
        start = start,
        end = start + link.label.length,
    )
}

internal fun legalUrl(baseUrl: String, path: String): String =
    "${baseUrl.trimEnd('/')}/$path"
