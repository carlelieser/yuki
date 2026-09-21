package app.yuki.core.designsystem.image

import android.content.Context
import coil3.ImageLoader
import coil3.svg.SvgDecoder

fun yukiImageLoader(context: Context): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(SvgDecoder.Factory()) }
        .build()
