package com.nanami.koishi.core.data.update

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val releaseJson = """
        {
          "tag_name": "v0.4.0",
          "name": "Koishi v0.4.0",
          "body": "Changelog details",
          "html_url": "https://github.com/NanamiKyoka/Koishi/releases/tag/v0.4.0",
          "assets": [
            {
              "name": "Koishi-v0.4.0-arm64-v8a.apk",
              "browser_download_url": "https://github.com/download/arm64.apk",
              "size": 1234567,
              "content_type": "application/vnd.android.package-archive"
            }
          ]
        }
    """.trimIndent()

    private class FakeUpdateCache(
        var body: String? = null,
        var checkedAt: Long = 0L
    ) : UpdateCache {
        override fun cachedReleaseBody(): String? = body
        override fun lastCheckedAt(): Long = checkedAt
        override fun save(releaseBody: String, checkedAt: Long) {
            body = releaseBody
            this.checkedAt = checkedAt
        }
    }

    private fun manager(server: MockWebServer, cache: UpdateCache = NoopUpdateCache): AppUpdateManager {
        return AppUpdateManager(
            httpClient = OkHttpClient(),
            cache = cache,
            apiBaseUrl = server.url("/").toString().removeSuffix("/")
        )
    }

    @Test
    fun parseAndCompareSemanticVersions() {
        val v020 = SemanticVersion.parse("v0.2.0")
        val v021 = SemanticVersion.parse("0.2.1")
        val v030 = SemanticVersion.parse("v0.3.0")
        val v100 = SemanticVersion.parse("1.0.0")
        val v020Debug = SemanticVersion.parse("0.2.0-debug")

        assertTrue(v021 > v020)
        assertTrue(v030 > v021)
        assertTrue(v100 > v030)
        assertEquals(0, v020.compareTo(v020Debug))
    }

    @Test
    fun selectBestAssetMatchesDeviceAbi() {
        val updateManager = AppUpdateManager()
        val assets = listOf(
            ReleaseAsset("Koishi-v0.2.0-arm64-v8a.apk", "http://example.com/arm64.apk", 1000L, ""),
            ReleaseAsset("Koishi-v0.2.0-armeabi-v7a.apk", "http://example.com/armv7.apk", 800L, ""),
            ReleaseAsset("Koishi-v0.2.0-universal.apk", "http://example.com/universal.apk", 1200L, ""),
            ReleaseAsset("Koishi-v0.2.0.aab", "http://example.com/bundle.aab", 1500L, "")
        )

        val arm64Match = updateManager.selectBestAsset(assets, arrayOf("arm64-v8a", "armeabi-v7a"))
        assertNotNull(arm64Match)
        assertEquals("Koishi-v0.2.0-arm64-v8a.apk", arm64Match?.name)

        val armv7Match = updateManager.selectBestAsset(assets, arrayOf("armeabi-v7a"))
        assertNotNull(armv7Match)
        assertEquals("Koishi-v0.2.0-armeabi-v7a.apk", armv7Match?.name)

        val fallbackMatch = updateManager.selectBestAsset(assets, arrayOf("x86_64"))
        assertNotNull(fallbackMatch)
        assertEquals("Koishi-v0.2.0-universal.apk", fallbackMatch?.name)
    }

    @Test
    fun selectBestAssetIgnoresNonApkFiles() {
        val updateManager = AppUpdateManager()
        val assets = listOf(
            ReleaseAsset("Koishi-v0.2.0.aab", "http://example.com/bundle.aab", 1500L, ""),
            ReleaseAsset("checksums.txt", "http://example.com/checksums.txt", 100L, "")
        )

        val match = updateManager.selectBestAsset(assets, arrayOf("arm64-v8a"))
        assertNull(match)
    }

    @Test
    fun deserializeGithubReleaseDto() {
        val jsonString = """
            {
              "tag_name": "v0.3.0",
              "name": "Koishi v0.3.0",
              "body": "Changelog details",
              "html_url": "https://github.com/NanamiKyoka/Koishi/releases/tag/v0.3.0",
              "assets": [
                {
                  "name": "Koishi-v0.3.0-arm64-v8a.apk",
                  "browser_download_url": "https://github.com/download/arm64.apk",
                  "size": 1234567,
                  "content_type": "application/vnd.android.package-archive"
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString<GithubReleaseDto>(jsonString)
        assertEquals("v0.3.0", dto.tagName)
        assertEquals("Koishi v0.3.0", dto.name)
        assertEquals(1, dto.assets.size)
        assertEquals("Koishi-v0.3.0-arm64-v8a.apk", dto.assets.first().name)
        assertEquals(1234567L, dto.assets.first().size)
    }

    @Test
    fun reusesFreshCacheWithoutHittingNetwork() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            val cache = FakeUpdateCache(body = releaseJson, checkedAt = System.currentTimeMillis())
            val result = manager(server, cache).checkForUpdate("0.3.0", arrayOf("arm64-v8a"))

            assertTrue(result is UpdateCheckResult.NewVersion)
            assertEquals(0, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun cachesSuccessfulResponse() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(200).setBody(releaseJson))
        server.start()
        try {
            val cache = FakeUpdateCache()
            val result = manager(server, cache).checkForUpdate("0.3.0", arrayOf("arm64-v8a"))

            assertTrue(result is UpdateCheckResult.NewVersion)
            assertEquals(releaseJson, cache.cachedReleaseBody())
            assertTrue(cache.lastCheckedAt() > 0L)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun fallsBackToCacheWhenRequestFails() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.start()
        try {
            val cache = FakeUpdateCache(body = releaseJson, checkedAt = 0L)
            val result = manager(server, cache).checkForUpdate("0.3.0", arrayOf("arm64-v8a"))

            assertTrue(result is UpdateCheckResult.NewVersion)
            assertEquals(1, server.requestCount)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun reportsUnavailableWhenRequestFailsWithoutCache() = runTest {
        val server = MockWebServer()
        server.enqueue(MockResponse().setResponseCode(403))
        server.start()
        try {
            val result = manager(server, FakeUpdateCache()).checkForUpdate("0.3.0", arrayOf("arm64-v8a"))

            assertTrue(result is UpdateCheckResult.Unavailable)
        } finally {
            server.shutdown()
        }
    }
}
