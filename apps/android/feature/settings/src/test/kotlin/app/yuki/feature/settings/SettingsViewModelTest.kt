package app.yuki.feature.settings

import android.Manifest
import app.cash.turbine.test
import app.yuki.core.model.UiState
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
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
            assertEquals(InstallMode.Automatic, successOf(awaitItem()).preferences.installMode)

            viewModel.onInstallModeChange(InstallMode.AlwaysAsk)

            assertEquals(InstallMode.AlwaysAsk, successOf(awaitItem()).preferences.installMode)
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

    private fun viewModel(): SettingsViewModel = SettingsViewModel(
        SettingsDependencies(shizuku = shizuku, store = store, reader = reader),
    )
}

private fun successOf(state: UiState<SettingsContent>): SettingsContent =
    (state as UiState.Success).data
