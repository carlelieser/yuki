package app.yuki.feature.listing

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.LinkOpener
import app.yuki.core.designsystem.component.OVERFLOW_MENU_TAG
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.Screenshot
import app.yuki.core.model.ScreenshotSelection
import app.yuki.core.model.UiState
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListingScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun resources() = InstrumentationRegistry.getInstrumentation().targetContext.resources

    private fun text(@StringRes id: Int, vararg args: Any): String = resources().getString(id, *args)

    private fun setScreen(
        listing: UiState<ListingUiModel>,
        status: ListingInstallStatus? = idleStatus(),
        callbacks: ListingScreenCallbacks = noopCallbacks(),
    ) {
        composeRule.setContent {
            ListingScreen(
                state = ListingScreenState(listing = listing, installStatus = status),
                callbacks = callbacks,
                onBackClick = {},
            )
        }
    }

    private fun setScreenWithActions(actions: List<ListingAction>) {
        composeRule.setContent {
            ListingScreen(
                state = ListingScreenState(
                    listing = UiState.Success(detail().toUiModel()),
                    installStatus = idleStatus(),
                    actions = actions,
                ),
                callbacks = noopCallbacks(),
                onBackClick = {},
            )
        }
    }

    @Test
    fun showsLoadingIndicatorWhileLoading() {
        setScreen(UiState.Loading)

        composeRule.onNodeWithTag(LISTING_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun showsFailureStateAndRetriesOnClick() {
        var retryCount = 0
        setScreen(
            listing = UiState.Failure(FailureReason.NotFound),
            callbacks = noopCallbacks().copy(onRetry = { retryCount += 1 }),
        )

        composeRule.onNodeWithTag(FAILURE_STATE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(text(DesignR.string.designsystem_failure_retry)).performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun rendersTheListingIdentityOnSuccess() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onNodeWithTag(LISTING_DETAIL_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(text(DesignR.string.designsystem_badge_author, "nightsky")).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(resources().getQuantityString(DesignR.plurals.designsystem_badge_stars, 128, 128)).assertIsDisplayed()
    }

    @Test
    fun rendersTheBannerWhenTheListingHasOne() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onNode(hasTestTag(LISTING_BANNER_TAG)).assertIsDisplayed()
    }

    @Test
    fun omitsTheBannerWhenTheListingHasNone() {
        setScreen(UiState.Success(detail(summary = summary(bannerUrl = null)).toUiModel()))

        composeRule.onAllNodesWithTag(LISTING_BANNER_TAG).assertCountEquals(0)
    }

    @Test
    fun showsArchivedWarningForAnArchivedListing() {
        setScreen(UiState.Success(detail(isArchived = true).toUiModel()))

        composeRule.scrollToText(text(R.string.listing_archived_title))
        composeRule.onNode(hasTestTag(ARCHIVED_WARNING_TAG)).assertIsDisplayed()
    }

    @Test
    fun hidesArchivedWarningForAnActiveListing() {
        setScreen(UiState.Success(detail(isArchived = false).toUiModel()))

        composeRule.onAllNodesWithText(text(R.string.listing_archived_title)).assertCountEquals(0)
    }

    @Test
    fun showsInstallButtonWhenAVersionIsInstallable() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).onFirst().assertIsDisplayed()
    }

    @Test
    fun replacesInstallButtonWithANoticeWhenNothingIsInstallable() {
        setScreen(UiState.Success(detail(versions = emptyList()).toUiModel()))

        composeRule.scrollToText(text(R.string.listing_no_installable_title))
        composeRule.onNode(hasTestTag(NO_INSTALLABLE_VERSION_TAG)).assertIsDisplayed()
        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).assertCountEquals(0)
    }

    @Test
    fun showsTheAppTitleOnlyOnceOnTheDetailScreen() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.onAllNodesWithText("Aurora").assertCountEquals(1)
    }

    @Test
    fun reportsTheTappedScreenshotIndex() {
        val selected = mutableListOf<ScreenshotSelection>()
        val screenshots = listOf(
            Screenshot("https://cdn.test/one.png", "Home"),
            Screenshot("https://cdn.test/two.png", "Settings"),
        )
        setScreen(
            listing = UiState.Success(detail(screenshots = screenshots).toUiModel()),
            callbacks = withScreenshotHandler(selected::add),
        )

        composeRule.scrollToText(text(R.string.listing_section_screenshots))
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(listOf(1), selected.map(ScreenshotSelection::index))
    }

    @Test
    fun reportsEveryScreenshotUrlSoTheViewerNeedsNoRefetch() {
        val selected = mutableListOf<ScreenshotSelection>()
        val screenshots = listOf(
            Screenshot("https://cdn.test/one.png", "Home"),
            Screenshot("https://cdn.test/two.png", "Settings"),
        )
        setScreen(
            listing = UiState.Success(detail(screenshots = screenshots).toUiModel()),
            callbacks = withScreenshotHandler(selected::add),
        )

        composeRule.scrollToText(text(R.string.listing_section_screenshots))
        composeRule.onNodeWithContentDescription("Settings").performClick()

        assertEquals(
            listOf(screenshots.map(Screenshot::url)),
            selected.map(ScreenshotSelection::urls),
        )
    }

    @Test
    fun installsTheExactVersionWhoseRowWasTapped() {
        val requested = mutableListOf<String>()
        val versions = listOf(version("v2.0.0"), version("v1.0.0"))
        setScreen(
            listing = UiState.Success(detail(versions = versions).toUiModel()),
            callbacks = withVersionInstallHandler { _, version -> requested.add(version.tag) },
        )

        composeRule.scrollToText("Aurora v1.0.0")
        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).onLast().performClick()

        assertEquals(listOf("v1.0.0"), requested)
    }

    @Test
    fun offersNoInstallButtonForAVersionWithoutAnAsset() {
        val versions = listOf(version("v2.0.0"), version("v1.0.0", downloadUrl = null))
        setScreen(UiState.Success(detail(versions = versions).toUiModel()))

        composeRule.scrollToText(text(R.string.listing_version_no_asset))
        composeRule.onNodeWithContentDescription(text(R.string.listing_version_no_asset)).assertIsDisplayed()
        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).assertCountEquals(1)
    }

    @Test
    fun installsAPrereleaseFromItsOwnRow() {
        val requested = mutableListOf<String>()
        val versions = listOf(version("v2.0.0"), version("v1.5.0-rc", isPrerelease = true))
        setScreen(
            listing = UiState.Success(detail(versions = versions).toUiModel()),
            callbacks = withVersionInstallHandler { _, version -> requested.add(version.tag) },
        )

        composeRule.scrollToText("Aurora v1.5.0-rc")
        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).onLast().performClick()

        assertEquals(listOf("v1.5.0-rc"), requested)
    }

    @Test
    fun withholdsTheInstallControlsUntilTheInstallStatusIsKnown() {
        setScreen(listing = UiState.Success(detail().toUiModel()), status = null)

        composeRule.onNodeWithText(text(DesignR.string.designsystem_install)).assertDoesNotExist()
        composeRule.onNodeWithText(text(DesignR.string.designsystem_install_uninstall)).assertDoesNotExist()
    }

    @Test
    fun showsTheInstallControlOnceTheStatusArrives() {
        setScreen(listing = UiState.Success(detail().toUiModel()), status = idleStatus())

        composeRule.onNodeWithText(text(DesignR.string.designsystem_install)).assertIsDisplayed()
    }

    @Test
    fun disablesOtherVersionRowsWhileOneVersionInstalls() {
        val versions = listOf(version("v2.0.0"), version("v1.0.0"))
        setScreen(
            listing = UiState.Success(detail(versions = versions).toUiModel()),
            status = ListingInstallStatus(
                state = InstallState.Downloading(HALF_DOWNLOADED),
                versionTag = "v2.0.0",
            ),
        )

        composeRule.scrollToText("Aurora v1.0.0")
        composeRule.onAllNodesWithText(text(DesignR.string.designsystem_install)).onLast().assertIsNotEnabled()
    }

    @Test
    fun forwardsTheInstallActionFromTheButton() {
        val actions = mutableListOf<String>()
        setScreen(
            listing = UiState.Success(detail().toUiModel()),
            callbacks = withInstallHandler { action -> actions.add(action.name) },
        )

        composeRule.scrollToText(text(DesignR.string.designsystem_install))
        composeRule.onNodeWithText(text(DesignR.string.designsystem_install)).performClick()

        assertEquals(listOf("Install"), actions)
    }

    @Test
    fun opensARepositoryLinkWhenItsRowIsTapped() {
        val opened = mutableListOf<String>()
        setScreen(
            listing = UiState.Success(detail().toUiModel()),
            callbacks = withLinkOpener { url -> opened.add(url) },
        )

        composeRule.scrollToText(text(R.string.listing_link_repository))
        composeRule.onNodeWithText(text(R.string.listing_link_repository)).performClick()

        assertEquals(listOf("https://github.com/nightsky/aurora"), opened)
    }

    @Test
    fun marksEveryLinkRowAsOpeningExternally() {
        setScreen(UiState.Success(detail().toUiModel()))

        composeRule.scrollToText(text(R.string.listing_link_license))
        composeRule.onAllNodesWithContentDescription(text(R.string.listing_external_link))
            .assertCountEquals(linkRows(detail()).size)
    }

    @Test
    fun rendersEachVersionInTheVersionList() {
        val versions = listOf(version("v2.0.0"), version("v1.0.0-rc", isPrerelease = true))
        setScreen(UiState.Success(detail(versions = versions).toUiModel()))

        composeRule.scrollToText("Aurora v2.0.0")
        composeRule.onNodeWithText("Aurora v2.0.0").assertIsDisplayed()

        composeRule.scrollToText("Aurora v1.0.0-rc")
        composeRule.onNodeWithText("Aurora v1.0.0-rc").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.listing_version_prerelease)).assertIsDisplayed()
    }

    @Test
    fun offersNoOverflowButtonWhenThereAreNoActions() {
        setScreenWithActions(emptyList())

        composeRule.onAllNodesWithTag(OVERFLOW_MENU_TAG).assertCountEquals(0)
    }

    @Test
    fun listsEveryActionWhenTheOverflowMenuOpens() {
        setScreenWithActions(
            listOf(
                ListingAction(label = "Share", onClick = {}),
                ListingAction(label = "Report", onClick = {}),
            ),
        )

        composeRule.onNodeWithTag(OVERFLOW_MENU_TAG).performClick()

        composeRule.onNodeWithText("Share").assertIsDisplayed()
        composeRule.onNodeWithText("Report").assertIsDisplayed()
    }

    @Test
    fun invokesTheTappedActionAndClosesTheMenu() {
        val invoked = mutableListOf<String>()
        setScreenWithActions(
            listOf(
                ListingAction(label = "Share", onClick = { invoked.add("Share") }),
                ListingAction(label = "Report", onClick = { invoked.add("Report") }),
            ),
        )

        composeRule.onNodeWithTag(OVERFLOW_MENU_TAG).performClick()
        composeRule.onNodeWithText("Report").performClick()

        assertEquals(listOf("Report"), invoked)
        composeRule.onAllNodesWithText("Share").assertCountEquals(0)
    }

    @Test
    fun ignoresTapsOnADisabledAction() {
        val invoked = mutableListOf<String>()
        setScreenWithActions(
            listOf(
                ListingAction(
                    label = "Share",
                    onClick = { invoked.add("Share") },
                    isEnabled = false,
                ),
            ),
        )

        composeRule.onNodeWithTag(OVERFLOW_MENU_TAG).performClick()
        composeRule.onNodeWithText("Share").assertIsNotEnabled()
        composeRule.onNodeWithText("Share").performClick()

        assertEquals(emptyList<String>(), invoked)
    }

    @Test
    fun showsTheAuthorSectionWhenTheAuthorHasOtherApps() {
        setAuthorSection(composeRule, listOf(otherApp()))

        composeRule.onNodeWithTag(LISTING_DETAIL_TAG)
            .performScrollToNode(hasText(text(R.string.listing_section_more_from, "nightsky")))
        composeRule.onNodeWithText(text(R.string.listing_section_more_from, "nightsky")).assertIsDisplayed()
        composeRule.onNodeWithText("Borealis").assertIsDisplayed()
    }

    @Test
    fun hidesTheAuthorSectionWhenTheAuthorHasNoOtherApps() {
        setAuthorSection(composeRule, emptyList())

        composeRule.onAllNodesWithText(text(R.string.listing_section_more_from, "nightsky")).assertCountEquals(0)
    }

    @Test
    fun theAuthorSectionArrowReportsItsAuthor() {
        val chosen = mutableListOf<String>()
        val base = noopCallbacks()
        setAuthorSection(
            rule = composeRule,
            listings = listOf(otherApp()),
            callbacks = base.copy(
                callbacks = base.callbacks.copy(onAuthorSelected = { chosen.add(it) }),
            ),
        )

        composeRule.onNodeWithTag(LISTING_DETAIL_TAG)
            .performScrollToNode(hasText(text(R.string.listing_section_more_from, "nightsky")))
        composeRule.onNodeWithContentDescription(text(DesignR.string.designsystem_section_see_all, "nightsky")).performClick()

        assertEquals(listOf("nightsky"), chosen)
    }

    @Test
    fun theAuthorSectionRowReportsTheSelectedListing() {
        val chosen = mutableListOf<String>()
        val base = noopCallbacks()
        setAuthorSection(
            rule = composeRule,
            listings = listOf(otherApp()),
            callbacks = base.copy(
                callbacks = base.callbacks.copy(onListingSelected = { chosen.add(it.slug) }),
            ),
        )

        composeRule.onNodeWithTag(LISTING_DETAIL_TAG)
            .performScrollToNode(hasText("Borealis"))
        composeRule.onNodeWithText("Borealis").performClick()

        assertEquals(listOf("borealis"), chosen)
    }
}

private fun idleStatus(): ListingInstallStatus =
    ListingInstallStatus(state = InstallState.NotInstalled, versionTag = null)

private fun ComposeContentTestRule.scrollToText(text: String) {
    onNode(hasTestTag(LISTING_DETAIL_TAG)).performScrollToNode(hasText(text))
}

private fun setAuthorSection(
    rule: ComposeContentTestRule,
    listings: List<ListingSummary>,
    callbacks: ListingScreenCallbacks = noopCallbacks(),
) {
    rule.setContent {
        ListingScreen(
            state = ListingScreenState(
                listing = UiState.Success(detail().toUiModel()),
                installStatus = idleStatus(),
                authored = AuthoredListings(listings = listings),
            ),
            callbacks = callbacks,
            onBackClick = {},
        )
    }
}

private fun otherApp(
    id: String = "listing-2",
    githubRepoId: Long = 43L,
    slug: String = "borealis",
    title: String = "Borealis",
) = summary(id = id, githubRepoId = githubRepoId, slug = slug, title = title)

private fun noopCallbacks(): ListingScreenCallbacks = ListingScreenCallbacks(
    callbacks = ListingCallbacks(
        onInstallAction = VersionInstallHandler { _, _ -> },
        onOpenLink = LinkOpener { },
        onScreenshotSelected = { },
        onVersionInstallAction = VersionInstallHandler { _, _ -> },
    ),
    onRetry = { },
)

private fun withInstallHandler(
    onAction: (app.yuki.core.designsystem.component.InstallAction) -> Unit,
): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(
        callbacks = base.callbacks.copy(
            onInstallAction = VersionInstallHandler { action, _ -> onAction(action) },
        ),
    )
}

private fun withLinkOpener(onOpen: (String) -> Unit): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(callbacks = base.callbacks.copy(onOpenLink = LinkOpener(onOpen)))
}

private fun withScreenshotHandler(
    onSelect: (ScreenshotSelection) -> Unit,
): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(callbacks = base.callbacks.copy(onScreenshotSelected = onSelect))
}

private fun withVersionInstallHandler(
    onAction: (app.yuki.core.designsystem.component.InstallAction, InstallableVersion) -> Unit,
): ListingScreenCallbacks {
    val base = noopCallbacks()
    return base.copy(
        callbacks = base.callbacks.copy(
            onVersionInstallAction = VersionInstallHandler(onAction),
        ),
    )
}

private val HALF_DOWNLOADED = downloadSizeOf(bytesDownloaded = 500L, bytesTotal = 1_000L)
