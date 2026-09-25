package app.yuki.feature.settings

import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.component.SearchBar
import app.yuki.core.designsystem.component.SearchBarState
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiTextButton
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import coil3.compose.AsyncImage
import coil3.request.ImageRequest

private val LIST_MAX_HEIGHT = 360.dp

private val DIALOG_PADDING = PaddingValues(
    start = YukiSpacing.ExtraLarge,
    top = YukiSpacing.ExtraLarge,
    end = YukiSpacing.ExtraLarge,
    bottom = YukiSpacing.Large,
)

internal data class InstallSourcePrompt(
    val apps: List<InstalledApp>?,
    val iconOf: suspend (String) -> Drawable?,
    val onSelect: (String) -> Unit,
    val onDismiss: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun InstallSourceDialog(prompt: InstallSourcePrompt) {
    BasicAlertDialog(
        onDismissRequest = prompt.onDismiss,
        modifier = Modifier.testTag(INSTALL_SOURCE_DIALOG_TAG),
    ) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(DIALOG_PADDING)) {
                InstallSourceBody(prompt = prompt)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = YukiSpacing.Small),
                    horizontalArrangement = Arrangement.End,
                ) {
                    YukiTextButton(
                        label = stringResource(R.string.settings_install_source_dismiss),
                        onClick = prompt.onDismiss,
                    )
                }
            }
        }
    }
}

@Composable
private fun InstallSourceBody(prompt: InstallSourcePrompt) {
    var query by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large)) {
        Text(
            text = stringResource(R.string.settings_install_source_title),
            style = MaterialTheme.typography.headlineSmall,
            color = AlertDialogDefaults.titleContentColor,
        )

        SearchBar(
            state = SearchBarState(query = query),
            onQueryChange = { next -> query = next },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        InstallSourceApps(apps = prompt.apps, query = query, prompt = prompt)
    }
}

@Composable
private fun InstallSourceApps(
    apps: List<InstalledApp>?,
    query: String,
    prompt: InstallSourcePrompt,
) {
    if (apps == null) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            YukiLoadingIndicator()
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = LIST_MAX_HEIGHT)
            .testTag(INSTALL_SOURCE_LIST_TAG),
    ) {
        items(items = matchingApps(apps, query), key = InstalledApp::packageName) { app ->
            InstalledAppRow(app = app, prompt = prompt)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun InstalledAppRow(app: InstalledApp, prompt: InstallSourcePrompt) {
    ListItem(
        onClick = { prompt.onSelect(app.packageName) },
        modifier = Modifier.fillMaxWidth(),
        leadingContent = {
            InstalledAppIcon(packageName = app.packageName, iconOf = prompt.iconOf)
        },
        supportingContent = { Text(text = app.packageName) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(vertical = YukiSpacing.Small),
    ) {
        Text(text = app.label)
    }
}

@Composable
private fun InstalledAppIcon(packageName: String, iconOf: suspend (String) -> Drawable?) {
    val icon by produceState<Drawable?>(initialValue = null, key1 = packageName) {
        value = iconOf(packageName)
    }

    Box(modifier = Modifier.size(YukiSize.IconSmall)) {
        val loaded = icon ?: return@Box

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(loaded).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(YukiSize.IconSmall),
        )
    }
}
