package app.yuki.core.designsystem

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

private const val ICON_DATA_URI = "data:image/svg+xml;base64," +
    "PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAxMDggMTA4Ij" +
    "48cGF0aCBkPSJNMCAwaDEwOHYxMDhIMHoiIGZpbGw9IiNmZjAwMDAiLz48L3N2Zz4="

@RunWith(AndroidJUnit4::class)
class SvgIconDecodingTest {
    @Test
    fun theSvgDataUriTheScraperProducesDecodesToAnImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val request = ImageRequest.Builder(context).data(ICON_DATA_URI).build()

        val result = runBlocking { ImageLoader(context).execute(request) }

        assertTrue("expected a decoded svg but got $result", result is SuccessResult)
    }
}
