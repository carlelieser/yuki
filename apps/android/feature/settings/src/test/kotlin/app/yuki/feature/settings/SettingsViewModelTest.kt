package app.yuki.feature.settings

import android.Manifest
import app.cash.turbine.test
import app.yuki.core.model.UiState
import app.yuki.core.shizuku.SHELL_INSTALLER_PACKAGE
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val shizuku = FakeShizukuSource()
    private val reader = FakePermissionStatusReader()
    private val store = FakePreferenceStore()
    private var apps = FakeInstalledAppsReader()

    @Before
    fun installDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun resetDispatcher() {
        Dispatchers.resetMain()
    }

    @Test
    fun theScreenReachesSuccessWithEveryPlannedPermissionRow() = runTest {
        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())
            val content = successOf(awaitItem())

            assertEquals(
                YUKI_PERMISSIONS.map(AppPermission::permission),
                content.permissions.map { row -> row.permission.permission },
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun aLiveShizukuChangeReachesTheCard() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(
                ShizukuActionKind.OpenWebsite,
                successOf(awaitItem()).card.actionKind,
            )

            shizuku.emit(detailOf(ShizukuState.Ready, ShizukuMode.Root, apiVersion = 13))

            val ready = successOf(awaitItem())
            assertEquals(null, ready.card.actionKind)
            assertEquals(MODE_ROOT, ready.modeLabel)
            assertEquals("API 13", ready.apiVersionLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun grantingAPermissionShowsOnTheNextResume() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(successOf(awaitItem()).permissions.none(PermissionRow::isGranted))

            reader.grant(Manifest.permission.INTERNET)
            viewModel.onResume()

            val granted = successOf(awaitItem()).permissions
                .first { row -> row.permission.permission == Manifest.permission.INTERNET }
            assertTrue(granted.isGranted)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resumingRefreshesShizukuBecauseTheUserCanChangeItWhileAway() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.onResume()
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, shizuku.refreshCount)
    }

    @Test
    fun includePrereleasesRoundTripsThroughTheStore() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertFalse(successOf(awaitItem()).preferences.includePrereleases)

            viewModel.onIncludePrereleasesChange(true)

            assertTrue(successOf(awaitItem()).preferences.includePrereleases)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun installModeRoundTripsThroughTheStore() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(InstallMode.Shizuku, successOf(awaitItem()).preferences.installMode)

            viewModel.onInstallModeChange(InstallMode.System)

            assertEquals(InstallMode.System, successOf(awaitItem()).preferences.installMode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun appearanceRoundTripsThroughTheStore() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(AppearanceMode.System, successOf(awaitItem()).preferences.appearance)

            viewModel.onAppearanceChange(AppearanceMode.Dark)

            assertEquals(AppearanceMode.Dark, successOf(awaitItem()).preferences.appearance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dynamicColorRoundTripsThroughTheStore() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(successOf(awaitItem()).preferences.isDynamicColorEnabled)

            viewModel.onDynamicColorChange(false)

            assertFalse(successOf(awaitItem()).preferences.isDynamicColorEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun requestingShizukuPermissionReachesTheMonitor() = runTest {
        viewModel().onRequestShizukuPermission()

        assertEquals(1, shizuku.permissionRequests)
    }

    @Test
    fun installerPackageRoundTripsThroughTheStore() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals(
                SHELL_INSTALLER_PACKAGE,
                successOf(awaitItem()).preferences.installerPackage,
            )

            viewModel.onInstallerPackageChange(PLAY_STORE_PACKAGE)

            assertEquals(
                PLAY_STORE_PACKAGE,
                successOf(awaitItem()).preferences.installerPackage,
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theSystemInstallerDisablesTheInstallSourceRow() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(successOf(awaitItem()).installSource.isEnabled)

            viewModel.onInstallModeChange(InstallMode.System)
            runCurrent()

            assertFalse(successOf(expectMostRecentItem()).installSource.isEnabled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theInstallSourceRowNamesThePresetBehindTheStoredPackage() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals("Shell", successOf(awaitItem()).installSource.label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theInstallSourceRowNamesAnInstalledAppBehindItsPackage() = runTest {
        apps = FakeInstalledAppsReader(listOf(installedAppOf("com.acme.store", "Acme Store")))
        val viewModel = viewModel()
        store.setInstallerPackage("com.acme.store")

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals("Acme Store", successOf(awaitItem()).installSource.label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theInstallSourceRowFallsBackToThePackageOfAnAbsentApp() = runTest {
        val viewModel = viewModel()
        store.setInstallerPackage("com.gone.app")

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertEquals("com.gone.app", successOf(awaitItem()).installSource.label)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playStoreIsOfferedOnlyWhenItIsInstalled() = runTest {
        apps = FakeInstalledAppsReader(listOf(installedAppOf(PLAY_STORE_PACKAGE, "Play Store")))

        viewModel().state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertTrue(successOf(awaitItem()).installSource.isPlayStoreInstalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun resumingRechecksWhetherPlayStoreIsInstalled() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem())
            assertFalse(successOf(awaitItem()).installSource.isPlayStoreInstalled)

            apps.install(installedAppOf(PLAY_STORE_PACKAGE, "Play Store"))
            viewModel.onResume()
            runCurrent()

            assertTrue(successOf(expectMostRecentItem()).installSource.isPlayStoreInstalled)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun theChooserLoadsInstalledAppsOnlyWhenItIsOpened() = runTest {
        apps = FakeInstalledAppsReader(listOf(installedAppOf("com.acme.app", "Acme")))
        val viewModel = viewModel()

        assertEquals(0, apps.reads)

        viewModel.chooser.test {
            assertEquals(null, awaitItem())

            viewModel.onChooseInstallerApp()

            assertEquals(null, awaitItem()?.apps)
            assertEquals(listOf("Acme"), awaitItem()?.apps?.map(InstalledApp::label))
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, apps.reads)
    }

    @Test
    fun dismissingTheChooserClosesIt() = runTest {
        val viewModel = viewModel()
        viewModel.onChooseInstallerApp()

        viewModel.onDismissInstallerChooser()

        assertEquals(null, viewModel.chooser.value)
    }

    private fun viewModel(): SettingsViewModel = SettingsViewModel(
        SettingsDependencies(
            shizuku = shizuku,
            store = store,
            readers = SettingsReaders(permissions = reader, apps = apps),
        ),
    )
}

private fun successOf(state: UiState<SettingsContent>): SettingsContent =
    (state as UiState.Success).data
