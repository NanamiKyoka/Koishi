package com.nanami.koishi.feature.tools.mini_apps.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiniAppDownloaderTest {

    @Test
    fun `only http and https links can be opened or downloaded`() {
        assertTrue(MiniAppDownloader.isHttpUrl("https://example.com/report.pdf"))
        assertTrue(MiniAppDownloader.isHttpUrl("http://example.com/report.pdf"))
        assertFalse(MiniAppDownloader.isHttpUrl("blob:https://example.com/1a2b"))
        assertFalse(MiniAppDownloader.isHttpUrl("data:text/plain;base64,QUJD"))
        assertFalse(MiniAppDownloader.isHttpUrl("example.com/report.pdf"))
    }

    @Test
    fun `mime type is inferred from the url extension`() {
        assertEquals("image/png", MiniAppDownloader.mimeTypeFor("https://example.com/a.png?size=2"))
        assertEquals("image/jpeg", MiniAppDownloader.mimeTypeFor("https://example.com/photo.JPG"))
        assertNull(MiniAppDownloader.mimeTypeFor("https://example.com/download"))
        assertNull(MiniAppDownloader.mimeTypeFor("https://example.com/file.xyz"))
    }

    @Test
    fun `content disposition wins over the url`() {
        val name = MiniAppDownloader.fileName(
            url = "https://example.com/download?id=7",
            contentDisposition = "attachment; filename=\"%E6%8A%A5%E5%91%8A%202026.pdf\"",
            mimeType = "application/pdf"
        )

        assertEquals("报告 2026.pdf", name)
    }

    @Test
    fun `url segment is used when the header carries no name`() {
        val name = MiniAppDownloader.fileName(
            url = "https://example.com/files/cover%20art.png?size=large#preview",
            contentDisposition = null,
            mimeType = "image/png"
        )

        assertEquals("cover art.png", name)
    }

    @Test
    fun `mime type provides the fallback extension`() {
        val name = MiniAppDownloader.fileName(
            url = "https://example.com/download/",
            contentDisposition = null,
            mimeType = "application/pdf"
        )

        assertTrue(name.startsWith("download_"))
        assertTrue(name.endsWith(".pdf"))
    }

    @Test
    fun `unknown mime types fall back to a generic extension`() {
        val name = MiniAppDownloader.fileName(
            url = "https://example.com/download/",
            contentDisposition = "attachment",
            mimeType = null
        )

        assertTrue(name.endsWith(".bin"))
    }

    @Test
    fun `characters that are invalid on disk are replaced`() {
        val name = MiniAppDownloader.fileName(
            url = "https://example.com/download",
            contentDisposition = "attachment; filename=\"a:b*c?.pdf\"",
            mimeType = "application/pdf"
        )

        assertEquals("a_b_c_.pdf", name)
    }
}
