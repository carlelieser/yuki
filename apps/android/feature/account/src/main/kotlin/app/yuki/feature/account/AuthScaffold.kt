package app.yuki.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.component.YukiLogo
import app.yuki.core.designsystem.component.YukiSnackbarHost
import app.yuki.core.designsystem.component.YukiTextButton
import app.yuki.core.designsystem.component.YukiTonalCircle
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

private val CONTENT_WIDTH = 384.dp
private const val LOGO_CONTAINER_ALPHA = 0.1f

@Composable
internal fun AuthScaffold(
    title: String,
    modifier: Modifier = Modifier,
    message: AuthMessage? = null,
    content: @Composable () -> Unit,
) {
    val hostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        if (message == null) return@LaunchedEffect

        val result = hostState.showSnackbar(
            message = message.text,
            actionLabel = message.action?.label,
            withDismissAction = true,
        )

        message.onShown()

        if (result == SnackbarResult.ActionPerformed) message.action?.onAction?.invoke()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { YukiSnackbarHost(hostState = hostState) },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = YukiSpacing.ExtraLarge, vertical = YukiSpacing.Section),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.widthIn(max = CONTENT_WIDTH).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
            ) {
                AuthHeading(title)
                content()
            }
        }
    }
}

@Composable
private fun ColumnScope.AuthHeading(title: String) {
    YukiTonalCircle(
        diameter = YukiSize.IconHero,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = LOGO_CONTAINER_ALPHA),
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .padding(bottom = YukiSpacing.ExtraLarge),
    ) {
        YukiLogo(size = YukiSize.IconHeroGlyph)
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
internal fun AuthFooterPrompt(
    prompt: String,
    actionLabel: String,
    onActionClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = prompt,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        YukiTextButton(label = actionLabel, onClick = onActionClick)
    }
}
