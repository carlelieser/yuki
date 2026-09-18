package app.yuki.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.theme.YukiSize
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter

const val AVATAR_TAG = "avatar"
const val AVATAR_EDIT_DESCRIPTION = "Change your picture"

private const val GLYPH_FRACTION = 0.5f
private const val BADGE_FRACTION = 0.34f
private const val BADGE_GLYPH_FRACTION = 0.62f

enum class AvatarSize { Small, Medium, Large }

data class AvatarContent(
    val imageUrl: String?,
    val displayName: String?,
)

@Composable
fun Avatar(
    content: AvatarContent,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.Medium,
    onEditClick: (() -> Unit)? = null,
) {
    val diameter = size.diameter()

    Box(modifier = modifier.size(diameter).testTag(AVATAR_TAG)) {
        AvatarFace(
            content = content,
            diameter = diameter,
            modifier = Modifier.editable(onEditClick),
        )

        if (onEditClick != null) {
            EditBadge(
                diameter = diameter * BADGE_FRACTION,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

private fun Modifier.editable(onEditClick: (() -> Unit)?): Modifier {
    val onClick = onEditClick ?: return this

    return clickable(
        onClick = onClick,
        role = Role.Button,
        onClickLabel = AVATAR_EDIT_DESCRIPTION,
    )
}

@Composable
private fun AvatarFace(
    content: AvatarContent,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val painter = content.imageUrl?.let { url ->
        rememberAsyncImagePainter(model = url, contentScale = ContentScale.Crop)
    }
    val isLoaded = painter?.state?.collectAsState()?.value is AsyncImagePainter.State.Success

    if (painter == null || !isLoaded) {
        AvatarFallback(displayName = content.displayName, diameter = diameter, modifier = modifier)
        return
    }

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .clearAndSetSemantics { },
    )
}

@Composable
private fun AvatarFallback(
    displayName: String?,
    diameter: Dp,
    modifier: Modifier = Modifier,
) {
    val initials = initialsOf(displayName)

    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (initials == null) {
            Icon(
                imageVector = YukiIcons.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(diameter * GLYPH_FRACTION),
            )
            return@Box
        }

        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EditBadge(diameter: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(diameter)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = YukiIcons.Edit,
            contentDescription = AVATAR_EDIT_DESCRIPTION,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(diameter * BADGE_GLYPH_FRACTION),
        )
    }
}

internal fun initialsOf(displayName: String?): String? {
    val words = displayName?.trim()?.split(" ")?.filter(String::isNotEmpty).orEmpty()
    if (words.isEmpty()) return null

    return words.take(2)
        .mapNotNull { word -> word.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { null }
}

private fun AvatarSize.diameter(): Dp = when (this) {
    AvatarSize.Small -> YukiSize.IconMedium
    AvatarSize.Medium -> YukiSize.IconLarge
    AvatarSize.Large -> YukiSize.IconExtraLarge
}
