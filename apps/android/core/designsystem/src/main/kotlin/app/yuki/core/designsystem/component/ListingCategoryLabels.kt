package app.yuki.core.designsystem.component

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.R
import app.yuki.core.model.ListingCategory

private val categoryLabels: Map<ListingCategory, Int> = mapOf(
    ListingCategory.SystemTweaks to R.string.designsystem_category_system_tweaks,
    ListingCategory.AppManagement to R.string.designsystem_category_app_management,
    ListingCategory.FileManagement to R.string.designsystem_category_file_management,
    ListingCategory.Media to R.string.designsystem_category_media,
    ListingCategory.Gaming to R.string.designsystem_category_gaming,
    ListingCategory.Automation to R.string.designsystem_category_automation,
    ListingCategory.Networking to R.string.designsystem_category_networking,
    ListingCategory.PrivacySecurity to R.string.designsystem_category_privacy_security,
    ListingCategory.DeveloperTools to R.string.designsystem_category_developer_tools,
    ListingCategory.DeviceSpecific to R.string.designsystem_category_device_specific,
    ListingCategory.Customization to R.string.designsystem_category_customization,
    ListingCategory.Connectivity to R.string.designsystem_category_connectivity,
    ListingCategory.Utilities to R.string.designsystem_category_utilities,
)

@StringRes
fun ListingCategory.labelRes(): Int = categoryLabels.getValue(this)

@Composable
fun ListingCategory.label(): String = stringResource(labelRes())

@Composable
fun categoryLabels(): Map<ListingCategory, String> =
    ListingCategory.entries.associateWith { category -> category.label() }
