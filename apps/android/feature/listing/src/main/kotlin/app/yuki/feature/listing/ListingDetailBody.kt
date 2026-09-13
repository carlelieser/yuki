package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.ScreenshotCarousel
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingVersion

const val LISTING_DETAIL_TAG = "listingDetail"

data class ListingCallbacks(
    val onInstallAction: InstallActionHandler,
    val onOpenLink: LinkOpener,
    val onScreenshotSelected: (Int) -> Unit,
    val onVersionInstallAction: VersionInstallHandler,
)

fun interface VersionInstallHandler {
    fun onAction(action: InstallAction, version: ListingVersion)
}

private fun LazyListScope.bannerSection(model: ListingUiModel) {
    val bannerUrl = model.detail.summary.bannerUrl ?: return

    item { ListingBanner(bannerUrl = bannerUrl) }
}

private fun LazyListScope.headerSection(model: ListingUiModel) {
    item { ListingHeader(summary = model.detail.summary) }
}

private fun LazyListScope.installSection(
    model: ListingUiModel,
    installState: InstallState,
    onInstallAction: InstallActionHandler,
) {
    if (!model.isInstallable) {
        item { NoInstallableVersionNotice(modifier = Modifier.padding(YukiSpacing.Large)) }
        return
    }

    item {
        InstallButton(
            state = installState,
            onAction = onInstallAction,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YukiSpacing.Large),
        )
    }
}

private fun LazyListScope.warningSection(model: ListingUiModel) {
    if (!model.detail.isArchived) return

    item { ArchivedWarning(modifier = Modifier.padding(horizontal = YukiSpacing.Large)) }
}

private fun LazyListScope.descriptionSection(model: ListingUiModel) {
    val description = model.detail.summary.description ?: return

    item { ListingDescription(description = description) }
}

private fun LazyListScope.screenshotSection(
    model: ListingUiModel,
    onScreenshotSelected: (Int) -> Unit,
) {
    val screenshots = model.detail.screenshots
    if (screenshots.isEmpty()) return

    item { SectionHeader(title = "Screenshots") }
    item { ScreenshotCarousel(screenshots = screenshots, onSelect = onScreenshotSelected) }
}

private fun LazyListScope.linkSection(model: ListingUiModel, onOpenLink: LinkOpener) {
    val rows = linkRows(model.detail)

    item { SectionHeader(title = "Links") }
    items(items = rows, key = ListingLinkRow::label) { row ->
        ListingLinkItem(row = row, onOpen = { onOpenLink.open(row.url) })
    }
}

private fun LazyListScope.versionSection(
    model: ListingUiModel,
    status: ListingInstallStatus,
    onVersionInstallAction: VersionInstallHandler,
) {
    val versions = model.detail.versions
    if (versions.isEmpty()) return

    item { SectionHeader(title = "Versions") }
    items(items = versions, key = { it.tag }) { version ->
        ListingVersionItem(
            version = version,
            installState = versionInstallState(version = version, status = status),
            onAction = InstallActionHandler { action ->
                onVersionInstallAction.onAction(action, version)
            },
        )
    }
}

@Composable
internal fun ListingDetailBody(
    model: ListingUiModel,
    status: ListingInstallStatus,
    callbacks: ListingCallbacks,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(LISTING_DETAIL_TAG),
        contentPadding = PaddingValues(vertical = YukiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
    ) {
        bannerSection(model)
        headerSection(model)
        installSection(model, status.state, callbacks.onInstallAction)
        warningSection(model)
        descriptionSection(model)
        screenshotSection(model, callbacks.onScreenshotSelected)
        linkSection(model, callbacks.onOpenLink)
        versionSection(model, status, callbacks.onVersionInstallAction)
    }
}
