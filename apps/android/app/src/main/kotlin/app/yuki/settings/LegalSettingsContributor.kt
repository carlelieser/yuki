package app.yuki.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.LinkOpener
import app.yuki.core.designsystem.component.rememberLinkOpener
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.network.YukiBaseUrl
import app.yuki.core.settings.api.SettingsContributor
import app.yuki.core.settings.api.SettingsGroup
import javax.inject.Inject

const val LEGAL_FOOTER_TAG = "legalFooter"

private const val TERMS_LABEL = "Terms of Service"
private const val PRIVACY_LABEL = "Privacy Policy"
private const val SEPARATOR = "•"
private const val TERMS_PATH = "terms"
private const val PRIVACY_PATH = "privacy"

class LegalSettingsContributor @Inject constructor(
    @param:YukiBaseUrl private val baseUrl: String,
) : SettingsContributor {
    override val group = SettingsGroup.Legal

    @Composable
    override fun Content() {
        LegalFooter(baseUrl = baseUrl, onLinkClick = rememberLinkOpener())
    }
}

@Composable
internal fun LegalFooter(
    baseUrl: String,
    onLinkClick: LinkOpener,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = YukiSpacing.Large)
            .testTag(LEGAL_FOOTER_TAG),
        horizontalArrangement = Arrangement.spacedBy(
            space = YukiSpacing.Small,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegalLink(
            label = PRIVACY_LABEL,
            url = legalUrl(baseUrl, PRIVACY_PATH),
            onLinkClick = onLinkClick,
        )

        Text(
            text = SEPARATOR,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LegalLink(
            label = TERMS_LABEL,
            url = legalUrl(baseUrl, TERMS_PATH),
            onLinkClick = onLinkClick,
        )
    }
}

@Composable
private fun LegalLink(label: String, url: String, onLinkClick: LinkOpener) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clickable { onLinkClick.open(url) },
    )
}

internal fun legalUrl(baseUrl: String, path: String): String = "${baseUrl.trimEnd('/')}/$path"
