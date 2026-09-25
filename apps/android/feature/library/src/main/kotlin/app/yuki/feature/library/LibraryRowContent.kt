package app.yuki.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.ProductListItemContent
import app.yuki.core.designsystem.component.installFailureLabel
import app.yuki.core.designsystem.component.label

private val fixedSupporting: Map<LibrarySupporting, Int> = mapOf(
    LibrarySupporting.UnknownVersion to R.string.library_supporting_unknown_version,
    LibrarySupporting.NotInstalled to R.string.library_supporting_not_installed,
    LibrarySupporting.Installing to R.string.library_supporting_installing,
    LibrarySupporting.Pending to R.string.library_supporting_pending,
)

@Composable
private fun LibrarySupporting.text(): String = when (this) {
    is LibrarySupporting.Version -> tag
    is LibrarySupporting.Download -> size.label()
    is LibrarySupporting.Failure -> stringResource(installFailureLabel(reason))
    LibrarySupporting.None -> ""
    else -> stringResource(fixedSupporting.getValue(this))
}

@Composable
internal fun LibraryItem.toListItem(): ProductListItemContent = ProductListItemContent(
    title = entry.title,
    supporting = supporting.text(),
    iconUrl = entry.iconUrl,
    installState = install,
)
