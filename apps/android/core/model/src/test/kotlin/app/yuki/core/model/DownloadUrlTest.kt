package app.yuki.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadUrlTest {
    @Test
    fun `builds a route url carrying the architecture`() {
        val url = downloadUrl("https://yukistore.org/", "my-app", "v1.2.0", "arm64-v8a")

        assertEquals("https://yukistore.org/listings/my-app/download/v1.2.0?arch=arm64-v8a", url)
    }

    @Test
    fun `omits the query when the architecture is unknown`() {
        val url = downloadUrl("https://yukistore.org", "my-app", "v1.2.0", null)

        assertEquals("https://yukistore.org/listings/my-app/download/v1.2.0", url)
    }

    @Test
    fun `encodes tags that contain url characters`() {
        val url = downloadUrl("https://yukistore.org", "my-app", "release/1.0", null)

        assertEquals("https://yukistore.org/listings/my-app/download/release%2F1.0", url)
    }

    @Test
    fun `takes the first supported abi as the device architecture`() {
        assertEquals("arm64-v8a", deviceArchitecture(listOf("arm64-v8a", "armeabi-v7a")))
    }

    @Test
    fun `has no architecture when the device reports none`() {
        assertNull(deviceArchitecture(emptyList()))
    }
}
