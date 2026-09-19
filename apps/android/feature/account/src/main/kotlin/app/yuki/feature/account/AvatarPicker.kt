package app.yuki.feature.account

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.yuki.core.network.AvatarUpload
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal const val AVATAR_CONTENT_TYPE = "image/webp"

private const val MAX_DIMENSION = 512
private const val QUALITY = 85

internal fun interface AvatarPicker {
    fun launch()
}

@Composable
internal fun rememberAvatarPicker(onPicked: (AvatarUpload) -> Unit): AvatarPicker {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val picked = uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            val upload = withContext(Dispatchers.IO) { readUpload(resolver, picked) }
            upload?.let(onPicked)
        }
    }

    return remember(launcher) {
        AvatarPicker {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        }
    }
}

private fun readUpload(resolver: ContentResolver, uri: Uri): AvatarUpload? {
    val decoded = resolver.openInputStream(uri)?.use(BitmapFactory::decodeStream) ?: return null

    return AvatarUpload(bytes = decoded.toWebp(), contentType = AVATAR_CONTENT_TYPE)
}

private fun Bitmap.toWebp(): ByteArray {
    val scaled = downscaled()
    val stream = ByteArrayOutputStream()

    scaled.compress(webpFormat(), QUALITY, stream)

    return stream.toByteArray()
}

private fun Bitmap.downscaled(): Bitmap {
    val longestSide = maxOf(width, height)
    if (longestSide <= MAX_DIMENSION) return this

    val scale = MAX_DIMENSION.toFloat() / longestSide

    return Bitmap.createScaledBitmap(
        this,
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
        true,
    )
}

@Suppress("DEPRECATION")
private fun webpFormat(): Bitmap.CompressFormat =
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.WEBP
    }
