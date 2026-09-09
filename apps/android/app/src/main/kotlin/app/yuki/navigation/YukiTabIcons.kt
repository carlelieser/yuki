package app.yuki.navigation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private fun tabIcon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

internal object YukiTabIcons {
    val Explore: ImageVector = tabIcon("Explore") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            reflectiveCurveToRelative(4.48f, 10f, 10f, 10f)
            reflectiveCurveToRelative(10f, -4.48f, 10f, -10f)
            reflectiveCurveTo(17.52f, 2f, 12f, 2f)
            close()
            moveTo(14.19f, 14.19f)
            lineTo(6f, 18f)
            lineToRelative(3.81f, -8.19f)
            lineTo(18f, 6f)
            close()
            moveTo(12f, 10.9f)
            curveToRelative(-0.61f, 0f, -1.1f, 0.49f, -1.1f, 1.1f)
            reflectiveCurveToRelative(0.49f, 1.1f, 1.1f, 1.1f)
            reflectiveCurveToRelative(1.1f, -0.49f, 1.1f, -1.1f)
            reflectiveCurveToRelative(-0.49f, -1.1f, -1.1f, -1.1f)
            close()
        }
    }

    val Library: ImageVector = tabIcon("Library") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(4f, 6f)
            horizontalLineTo(2f)
            verticalLineToRelative(14f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(4f)
            close()
            moveTo(20f, 2f)
            horizontalLineTo(8f)
            curveToRelative(-1.1f, 0f, -2f, 0.9f, -2f, 2f)
            verticalLineToRelative(12f)
            curveToRelative(0f, 1.1f, 0.9f, 2f, 2f, 2f)
            horizontalLineToRelative(12f)
            curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
            verticalLineTo(4f)
            curveToRelative(0f, -1.1f, -0.9f, -2f, -2f, -2f)
            close()
            moveTo(20f, 16f)
            horizontalLineTo(8f)
            verticalLineTo(4f)
            horizontalLineToRelative(12f)
            close()
        }
    }

    val Updates: ImageVector = tabIcon("Updates") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 4f)
            verticalLineTo(1f)
            lineTo(8f, 5f)
            lineToRelative(4f, 4f)
            verticalLineTo(6f)
            curveToRelative(3.31f, 0f, 6f, 2.69f, 6f, 6f)
            curveToRelative(0f, 1.01f, -0.25f, 1.97f, -0.7f, 2.8f)
            lineToRelative(1.46f, 1.46f)
            curveTo(19.54f, 15.03f, 20f, 13.57f, 20f, 12f)
            curveToRelative(0f, -4.42f, -3.58f, -8f, -8f, -8f)
            close()
            moveTo(12f, 18f)
            curveToRelative(-3.31f, 0f, -6f, -2.69f, -6f, -6f)
            curveToRelative(0f, -1.01f, 0.25f, -1.97f, 0.7f, -2.8f)
            lineTo(5.24f, 7.74f)
            curveTo(4.46f, 8.97f, 4f, 10.43f, 4f, 12f)
            curveToRelative(0f, 4.42f, 3.58f, 8f, 8f, 8f)
            verticalLineToRelative(3f)
            lineToRelative(4f, -4f)
            lineToRelative(-4f, -4f)
            close()
        }
    }

}
