package app.yuki.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

@Composable
internal fun ShizukuApiVersion.text(): String = when (this) {
    ShizukuApiVersion.Unknown -> stringResource(R.string.settings_shizuku_api_unknown)
    is ShizukuApiVersion.Known -> stringResource(R.string.settings_shizuku_api_version, level)
}

@Composable
internal fun ShizukuCardDescription.text(): String = when (this) {
    is ShizukuCardDescription.Fixed -> stringResource(text)
    is ShizukuCardDescription.Ready -> stringResource(
        R.string.settings_card_ready_description,
        stringResource(modeLabelFor(mode)),
        apiVersion.text(),
    )
}

@Composable
internal fun InstallSourceName.text(): String = when (this) {
    is InstallSourceName.Preset -> stringResource(label)
    is InstallSourceName.App -> label
    is InstallSourceName.Package -> packageName
}
