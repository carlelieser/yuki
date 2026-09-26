package app.yuki.feature.account

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.scale
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

internal sealed interface AvatarPick {
    data class Ready(val upload: AvatarUpload) : AvatarPick

    data object Unreadable : AvatarPick
}

@Composable
internal fun rememberAvatarPicker(onPicked: (AvatarPick) -> Unit): AvatarPicker {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val picked = uri ?: return@rememberLauncherForActivityResult

        scope.launch {
            val pick = withContext(Dispatchers.IO) { readUpload(resolver, picked) }
            onPicked(pick)
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

private fun readUpload(resolver: ContentResolver, uri: Uri): AvatarPick {
    val decoded = runCatching { decode(resolver, uri) }.getOrNull()
        ?: return AvatarPick.Unreadable

    val bytes = runCatching { decoded.toWebp() }.getOrNull() ?: return AvatarPick.Unreadable

    return AvatarPick.Ready(AvatarUpload(bytes = bytes, contentType = AVATAR_CONTENT_TYPE))
}

private fun decode(resolver: ContentResolver, uri: Uri): Bitmap? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        decodeWithImageDecoder(resolver, uri)
    } else {
        resolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }

@RequiresApi(Build.VERSION_CODES.P)
private fun decodeWithImageDecoder(resolver: ContentResolver, uri: Uri): Bitmap =
    ImageDecoder.decodeBitmap(
        ImageDecoder.createSource(resolver, uri),
    ) { decoder, info, _ ->
        decoder.isMutableRequired = false
        decoder.setTargetSampleSize(sampleSizeFor(info.size.width, info.size.height))
    }

private fun sampleSizeFor(width: Int, height: Int): Int {
    var sampleSize = 1
    while (maxOf(width, height) / (sampleSize * 2) >= MAX_DIMENSION) {
        sampleSize *= 2
    }

    return sampleSize
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

    return scale(
        (width * scale).toInt().coerceAtLeast(1),
        (height * scale).toInt().coerceAtLeast(1),
    )
}

@Suppress("DEPRECATION")
private fun webpFormat(): Bitmap.CompressFormat =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        Bitmap.CompressFormat.WEBP
    }
