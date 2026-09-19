package app.yuki.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiSize

const val YUKI_LOGO_TAG = "yukiLogo"

@Composable
fun YukiLogo(
    modifier: Modifier = Modifier,
    size: Dp = YukiSize.IconLarge,
) {
    Image(
        painter = painterResource(R.drawable.ic_yuki_logo),
        contentDescription = null,
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { }
            .testTag(YUKI_LOGO_TAG),
    )
}
