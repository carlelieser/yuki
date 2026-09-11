package app.yuki.feature.listing

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import app.yuki.core.designsystem.component.YukiIcons

internal val ListingLinkKind.icon: ImageVector
    @Composable get() = when (this) {
        ListingLinkKind.Repository -> YukiIcons.Repository
        ListingLinkKind.Author -> YukiIcons.Person
        ListingLinkKind.Homepage -> YukiIcons.Homepage
        ListingLinkKind.License -> YukiIcons.License
    }
