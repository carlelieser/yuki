package app.yuki.core.model

import android.os.Build
import java.net.URLEncoder

fun deviceArchitecture(abis: List<String> = Build.SUPPORTED_ABIS.orEmpty().toList()): String? =
    abis.firstOrNull()

fun downloadUrl(baseUrl: String, slug: String, versionTag: String, architecture: String?): String {
    val origin = baseUrl.trimEnd('/')
    val path = "$origin/listings/${encode(slug)}/download/${encode(versionTag)}"

    return if (architecture == null) path else "$path?arch=${encode(architecture)}"
}

private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
