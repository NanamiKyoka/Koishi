package com.nanami.koishi.feature.tools.mini_apps.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiniAppUrlTest {

    @Test
    fun `scheme is filled in and normalized to https`() {
        assertEquals("https://example.com", MiniAppUrl.normalize("example.com"))
        assertEquals("https://example.com", MiniAppUrl.normalize("  http://Example.com  "))
    }

    @Test
    fun `path and query are preserved while the host is lowercased`() {
        assertEquals(
            "https://www.example.com/a/b?c=1",
            MiniAppUrl.normalize("https://www.EXAMPLE.com/a/b?c=1")
        )
    }

    @Test
    fun `explicit ports are kept except the default https port`() {
        assertEquals("https://example.com:8443/app", MiniAppUrl.normalize("example.com:8443/app"))
        assertEquals("https://example.com/app", MiniAppUrl.normalize("https://example.com:443/app"))
    }

    @Test
    fun `credentials are dropped`() {
        assertEquals("https://example.com/x", MiniAppUrl.normalize("https://user:pass@example.com/x"))
    }

    @Test
    fun `blank and malformed inputs are rejected`() {
        assertNull(MiniAppUrl.normalize(""))
        assertNull(MiniAppUrl.normalize("   "))
        assertNull(MiniAppUrl.normalize("not a url"))
        assertNull(MiniAppUrl.normalize("https://"))
        assertNull(MiniAppUrl.normalize("https:///only/path"))
        assertNull(MiniAppUrl.normalize("example.com:99999/x"))
    }

    @Test
    fun `non web schemes are rejected`() {
        assertNull(MiniAppUrl.normalize("ftp://example.com"))
        assertNull(MiniAppUrl.normalize("intent://example.com"))
    }

    @Test
    fun `local and international hosts are accepted`() {
        assertEquals("https://localhost:8080", MiniAppUrl.normalize("localhost:8080"))
        assertEquals("https://例子.测试", MiniAppUrl.normalize("例子.测试"))
    }

    @Test
    fun `host parsing ignores path and query`() {
        assertEquals("example.com", MiniAppUrl.hostOf("https://example.com/a/b?c=1#d"))
        assertEquals("m.example.com", MiniAppUrl.hostOf("https://m.example.com:8443/a"))
        assertNull(MiniAppUrl.hostOf("example.com"))
    }

    @Test
    fun `site domain strips the www prefix only`() {
        assertEquals("example.com", MiniAppUrl.siteDomainOf("https://www.example.com/x"))
        assertEquals("m.example.com", MiniAppUrl.siteDomainOf("https://m.example.com/x"))
        assertNull(MiniAppUrl.siteDomainOf("example.com"))
    }

    @Test
    fun `favicon url is derived from the host`() {
        assertEquals("https://example.com/favicon.ico", MiniAppUrl.faviconUrl("https://example.com/a"))
        assertNull(MiniAppUrl.faviconUrl("example.com"))
    }

    @Test
    fun `page titles are only accepted when they carry real information`() {
        assertFalse(MiniAppUrl.isMeaningfulTitle("", "https://example.com"))
        assertFalse(MiniAppUrl.isMeaningfulTitle("   ", "https://example.com"))
        assertFalse(MiniAppUrl.isMeaningfulTitle("https://example.com/a", "https://example.com"))
        assertFalse(MiniAppUrl.isMeaningfulTitle("example.com", "https://example.com"))
        assertTrue(MiniAppUrl.isMeaningfulTitle("Example Site", "https://example.com"))
    }
}
