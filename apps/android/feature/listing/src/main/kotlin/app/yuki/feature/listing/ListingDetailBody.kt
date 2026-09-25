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
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.InstallAction
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.InstallButton
import app.yuki.core.designsystem.component.LinkOpener
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.ListingSectionActions
import app.yuki.core.designsystem.component.ListingSectionContent
import app.yuki.core.designsystem.component.ScreenshotCarousel
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.listingSection
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.ScreenshotSelection

const val LISTING_DETAIL_TAG = "listingDetail"
const val MORE_FROM_AUTHOR_KEY = "moreFromAuthor"

data class ListingCallbacks(
    val onInstallAction: VersionInstallHandler,
    val onOpenLink: LinkOpener,
    val onScreenshotSelected: (ScreenshotSelection) -> Unit,
    val onVersionInstallAction: VersionInstallHandler,
    val onAuthorSelected: ((String) -> Unit)? = null,
    val onListingSelected: (ListingSummary) -> Unit = {},
)

data class AuthoredListings(
    val listings: List<ListingSummary> = emptyList(),
    val installs: ListingInstalls = ListingInstalls(),
)

fun interface VersionInstallHandler {
    fun onAction(action: InstallAction, version: InstallableVersion)
}

data class ListingUninstallStatus(
    val install: ListingInstallStatus?,
    val hasUninstallFailed: Boolean = false,
)

private fun LazyListScope.bannerSection(model: ListingUiModel) {
    val bannerUrl = model.detail.summary.bannerUrl ?: return

    item { ListingBanner(bannerUrl = bannerUrl) }
}

private fun LazyListScope.headerSection(
    model: ListingUiModel,
    onAuthorSelected: ((String) -> Unit)?,
) {
    item {
        ListingHeader(
            summary = model.detail.summary,
            onAuthorClick = onAuthorSelected,
        )
    }
}

private fun LazyListScope.installSection(
    model: ListingUiModel,
    status: ListingUninstallStatus,
    onInstallAction: VersionInstallHandler,
) {
    val hasUninstallFailed = status.hasUninstallFailed
    val installable = model.installableVersion
    if (installable == null) {
        item { NoInstallableVersionNotice(modifier = Modifier.padding(YukiSpacing.Large)) }
        return
    }

    val install = status.install ?: return

    item {
        InstallButton(
            state = install.state,
            onAction = InstallActionHandler { action ->
                onInstallAction.onAction(action, installable)
            },
            canUninstall = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = YukiSpacing.Large),
        )
    }

    if (!hasUninstallFailed) return

    item { UninstallFailedNotice(modifier = Modifier.padding(horizontal = YukiSpacing.Large)) }
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
    onScreenshotSelected: (ScreenshotSelection) -> Unit,
) {
    val screenshots = model.detail.screenshots
    if (screenshots.isEmpty()) return

    item { SectionHeader(title = stringResource(R.string.listing_section_screenshots)) }
    item { ScreenshotCarousel(screenshots = screenshots, onSelect = onScreenshotSelected) }
}

internal data class AuthorSection(val author: String, val title: String)

private fun LazyListScope.moreFromAuthorSection(
    section: AuthorSection,
    authored: AuthoredListings,
    callbacks: ListingCallbacks,
) {
    if (authored.listings.isEmpty()) return

    val author = section.author
    val onAuthorSelected = callbacks.onAuthorSelected

    listingSection(
        content = ListingSectionContent(
            title = section.title,
            keyPrefix = MORE_FROM_AUTHOR_KEY,
            listings = authored.listings,
            installs = authored.installs,
            seeAllLabel = author,
        ),
        actions = ListingSectionActions(
            onListingSelected = callbacks.onListingSelected,
            onSeeAll = onAuthorSelected?.let { select -> { select(author) } },
        ),
    )
}

private fun LazyListScope.linkSection(model: ListingUiModel, onOpenLink: LinkOpener) {
    val rows = linkRows(model.detail)

    item { SectionHeader(title = stringResource(R.string.listing_section_links)) }
    items(items = rows, key = ListingLinkRow::kind) { row ->
        ListingLinkItem(
            row = row,
            onOpen = { onOpenLink.open(row.url) },
            modifier = Modifier.animateItem(),
        )
    }
}

private fun LazyListScope.versionSection(
    model: ListingUiModel,
    status: ListingInstallStatus?,
    onVersionInstallAction: VersionInstallHandler,
) {
    val versions = model.detail.versions
    if (versions.isEmpty() || status == null) return

    item { SectionHeader(title = stringResource(R.string.listing_section_versions)) }
    items(items = versions, key = { it.tag }) { version ->
        ListingVersionItem(
            version = version,
            install = version.toInstallable()?.let { installable ->
                VersionInstallPresentation(
                    state = versionInstallState(version = version, status = status),
                    onAction = InstallActionHandler { action ->
                        onVersionInstallAction.onAction(action, installable)
                    },
                )
            },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
internal fun ListingDetailBody(
    model: ListingUiModel,
    status: ListingUninstallStatus,
    callbacks: ListingCallbacks,
    authored: AuthoredListings = AuthoredListings(),
) {
    val author = model.detail.summary.author
    val authorSection = AuthorSection(
        author = author,
        title = stringResource(R.string.listing_section_more_from, author),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(LISTING_DETAIL_TAG),
        contentPadding = PaddingValues(vertical = YukiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
    ) {
        bannerSection(model)
        headerSection(model, callbacks.onAuthorSelected)
        installSection(model, status, callbacks.onInstallAction)
        warningSection(model)
        descriptionSection(model)
        screenshotSection(model, callbacks.onScreenshotSelected)
        moreFromAuthorSection(authorSection, authored, callbacks)
        linkSection(model, callbacks.onOpenLink)
        versionSection(model, status.install, callbacks.onVersionInstallAction)
    }
}
