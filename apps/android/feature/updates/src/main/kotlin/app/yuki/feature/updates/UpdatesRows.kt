package app.yuki.feature.updates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.InstallProgressPosition
import app.yuki.core.designsystem.component.ProductListItem
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.FailureReason
import kotlin.reflect.KClass

const val UNCHECKED_ROW_TAG = "uncheckedRow"

data class UpdatesActions(
    val onListingClick: (String) -> Unit,
    val onInstallAction: (Long, InstallAction) -> Unit,
    val onRetry: () -> Unit,
)

internal fun LazyListScope.updateRows(content: UpdatesContent, actions: UpdatesActions) {
    items(content.updates, key = UpdateRow::githubRepoId) { row ->
        ProductListItem(
            content = row.toListItem(),
            modifier = Modifier
                .animateItem()
                .clickable { actions.onListingClick(row.slug) },
            trailing = {
                InstallButton(
                    state = row.install,
                    onAction = InstallActionHandler { action ->
                        actions.onInstallAction(row.githubRepoId, action)
                    },
                    progressPosition = InstallProgressPosition.None,
                )
            },
        )
    }
}

internal fun LazyListScope.uncheckedRows(content: UpdatesContent, actions: UpdatesActions) {
    if (content.unchecked.isEmpty()) return

    item { SectionHeader(title = stringResource(R.string.updates_unchecked_title)) }

    items(content.unchecked, key = UncheckedApp::githubRepoId) { entry ->
        UncheckedRow(
            entry = entry,
            onClick = { actions.onListingClick(entry.slug) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun UncheckedRow(
    entry: UncheckedApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProductListItem(
        content = entry.listItem,
        modifier = modifier.clickable(onClick = onClick).testTag(UNCHECKED_ROW_TAG),
        trailing = { UncheckedNote(reason = entry.reason) },
    )
}

@Composable
private fun UncheckedNote(reason: FailureReason) {
    Column(modifier = Modifier.padding(start = YukiSpacing.Small)) {
        Text(
            text = stringResource(R.string.updates_unchecked_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
        )
        Text(
            text = uncheckedDetail(reason),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private val uncheckedDetails: Map<KClass<out FailureReason>, Int> = mapOf(
    FailureReason.Offline::class to R.string.updates_unchecked_offline,
    FailureReason.NotFound::class to R.string.updates_unchecked_not_found,
    FailureReason.Unauthorized::class to R.string.updates_unchecked_sign_in,
    FailureReason.EmailNotVerified::class to R.string.updates_unchecked_sign_in,
    FailureReason.AccountExists::class to R.string.updates_unchecked_unexpected,
    FailureReason.Unexpected::class to R.string.updates_unchecked_unexpected,
)

@Composable
internal fun uncheckedDetail(reason: FailureReason): String = when (reason) {
    is FailureReason.Rejected -> reason.explanation
    is FailureReason.Server -> stringResource(R.string.updates_unchecked_server, reason.status)
    else -> stringResource(uncheckedDetails.getValue(reason::class))
}
