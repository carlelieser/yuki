package app.yuki.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import app.yuki.core.designsystem.R

object YukiIcons {
    val Search: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_search)

    val Error: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_error)

    val Library: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_library)

    val Update: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_update)

    val Settings: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_settings)

    val Back: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_arrow_back)

    val Forward: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_arrow_forward)

    val Close: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_close)

    val Explore: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_explore)

    val DeployedCode: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_deployed_code)

    val ArrowOutward: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_arrow_outward)

    val GridView: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_grid_view)

    val Sort: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_sort)
}
