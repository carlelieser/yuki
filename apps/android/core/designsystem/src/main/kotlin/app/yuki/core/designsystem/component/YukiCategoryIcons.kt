package app.yuki.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import app.yuki.core.designsystem.R

object YukiCategoryIcons {
    val SystemTweaks: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_tune)

    val AppManagement: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_inventory_2)

    val FileManagement: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_folder)

    val Media: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_movie)

    val Gaming: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_sports_esports)

    val Automation: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_account_tree)

    val Networking: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_lan)

    val PrivacySecurity: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_gpp_good)

    val DeveloperTools: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_code)

    val DeviceSpecific: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_smartphone)

    val Customization: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_palette)

    val Connectivity: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_bluetooth)

    val Utilities: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_handyman)

    val AllCategories: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_grid_view)
}
