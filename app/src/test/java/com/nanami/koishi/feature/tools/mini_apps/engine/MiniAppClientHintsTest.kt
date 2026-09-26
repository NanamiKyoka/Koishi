package com.nanami.koishi.feature.tools.mini_apps.engine

import androidx.webkit.UserAgentMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MiniAppClientHintsTest {

    private val mobileUserAgent =
        "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/131.0.0.0 Mobile Safari/537.36"

    private fun mobileProfile() = requireNotNull(
        MiniAppClientHints.profile(
            userAgent = mobileUserAgent,
            desktopMode = false,
            deviceRelease = "14",
            deviceModel = "Pixel 7"
        )
    )

    @Test
    fun `mobile profile announces google chrome like the browser does`() {
        val profile = mobileProfile()

        assertEquals(
            listOf(
                MiniAppClientHints.Brand("Not_A Brand", "24", "24.0.0.0"),
                MiniAppClientHints.Brand("Chromium", "131", "131.0.0.0"),
                MiniAppClientHints.Brand("Google Chrome", "131", "131.0.0.0")
            ),
            profile.brands
        )
        assertEquals("131.0.0.0", profile.fullVersion)
        assertEquals("Android", profile.platform)
        assertEquals("14", profile.platformVersion)
        assertEquals("Pixel 7", profile.model)
        assertTrue(profile.mobile)
        assertEquals(UserAgentMetadata.FORM_FACTOR_MOBILE, profile.formFactor)
    }

    @Test
    fun `desktop profile follows the desktop user agent`() {
        val profile = MiniAppClientHints.profile(
            userAgent = MiniAppUserAgent.desktopUserAgent(mobileUserAgent),
            desktopMode = true,
            deviceRelease = "14",
            deviceModel = "Pixel 7"
        )

        assertEquals("Windows", profile?.platform)
        assertEquals("10.0.0", profile?.platformVersion)
        assertEquals("", profile?.model)
        assertFalse(profile?.mobile == true)
        assertEquals(UserAgentMetadata.FORM_FACTOR_DESKTOP, profile?.formFactor)
        assertEquals("131", profile?.brands?.last()?.version)
    }

    @Test
    fun `profile is skipped when the user agent has no chrome version`() {
        assertNull(
            MiniAppClientHints.profile(
                userAgent = "Koishi/1.0",
                desktopMode = false,
                deviceRelease = "14",
                deviceModel = "Pixel 7"
            )
        )
    }

    @Test
    fun `metadata carries chrome brands with the full versions the builder requires`() {
        val metadata = mobileProfile().toUserAgentMetadata(formFactorsSupported = false)

        assertEquals(3, metadata.brandVersionList.size)
        assertTrue(
            metadata.brandVersionList.all { brand ->
                !brand.brand.isNullOrBlank() &&
                        !brand.majorVersion.isNullOrBlank() &&
                        !brand.fullVersion.isNullOrBlank()
            }
        )
        assertEquals("Google Chrome", metadata.brandVersionList.last().brand)
        assertEquals("131", metadata.brandVersionList.last().majorVersion)
        assertEquals("131.0.0.0", metadata.brandVersionList.last().fullVersion)
        assertEquals("131.0.0.0", metadata.fullVersion)
        assertEquals("Android", metadata.platform)
        assertEquals("14", metadata.platformVersion)
        assertEquals("Pixel 7", metadata.model)
        assertTrue(metadata.isMobile)
    }
}
