package com.nanami.koishi.feature.tools.mini_apps.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiniAppUserAgentTest {

    private val mobileUserAgent =
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "; wv) Chrome/120.0.6099.230 Mobile Safari/537.36"

    @Test
    fun `sanitize drops the wv marker`() {
        assertEquals(
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) ) Chrome/120.0.6099.230 Mobile Safari/537.36",
            MiniAppUserAgent.sanitize(mobileUserAgent)
        )
    }

    @Test
    fun `desktop user agent keeps the device chrome version`() {
        val desktop = MiniAppUserAgent.desktopUserAgent(MiniAppUserAgent.sanitize(mobileUserAgent))

        assertTrue(desktop.contains("Windows NT 10.0"))
        assertTrue(desktop.contains("Chrome/120.0.0.0"))
        assertFalse(desktop.contains("Mobile Safari"))
    }

    @Test
    fun `desktop user agent falls back when no chrome version is found`() {
        val desktop = MiniAppUserAgent.desktopUserAgent("Koishi/1.0")

        assertTrue(desktop.contains("Chrome/120.0.0.0"))
        assertTrue(desktop.contains("Windows NT 10.0"))
    }

    @Test
    fun `desktop scale fits the layout width into the view`() {
        assertEquals(0.402f, MiniAppUserAgent.desktopScale(412f), 0.001f)
        assertEquals(1f, MiniAppUserAgent.desktopScale(2000f), 0.001f)
    }

    @Test
    fun `desktop scale never shrinks below the minimum`() {
        assertEquals(0.25f, MiniAppUserAgent.desktopScale(60f), 0.001f)
    }

    @Test
    fun `viewport script pins the layout width and initial scale`() {
        val script = MiniAppUserAgent.desktopViewportScript(412f)

        assertTrue(script.contains("width=1024"))
        assertTrue(script.contains("initial-scale=0.402"))
        assertTrue(script.contains("minimum-scale=0.25"))
        assertTrue(script.contains("user-scalable=yes"))
    }

    @Test
    fun `viewport script handles pages without a viewport meta`() {
        val script = MiniAppUserAgent.desktopViewportScript(412f)

        assertTrue(script.contains("createElement('meta')"))
        assertTrue(script.contains("document.head.appendChild(meta)"))
    }
}
