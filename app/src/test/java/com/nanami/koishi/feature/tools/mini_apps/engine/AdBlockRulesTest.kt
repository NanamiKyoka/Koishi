package com.nanami.koishi.feature.tools.mini_apps.engine

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AdBlockRulesTest {

    @Test
    fun `ad host suffixes match the domain itself and its subdomains`() {
        assertTrue(AdBlockRules.isBlockedHost("doubleclick.net"))
        assertTrue(AdBlockRules.isBlockedHost("ad.doubleclick.net"))
        assertTrue(AdBlockRules.isBlockedHost("hm.baidu.com"))
    }

    @Test
    fun `matching is case insensitive and tolerant of surrounding dots`() {
        assertTrue(AdBlockRules.isBlockedHost("  DOUBLECLICK.NET.  "))
    }

    @Test
    fun `ordinary hosts and blank values are not blocked`() {
        assertFalse(AdBlockRules.isBlockedHost(null))
        assertFalse(AdBlockRules.isBlockedHost(""))
        assertFalse(AdBlockRules.isBlockedHost("example.com"))
        assertFalse(AdBlockRules.isBlockedHost("qq.com"))
        assertFalse(AdBlockRules.isBlockedHost("notdoubleclick.net"))
    }

    @Test
    fun `tracking requests are blocked by host`() {
        assertTrue(AdBlockRules.shouldBlockRequest("https://hm.baidu.com/hm.js?abc"))
        assertTrue(AdBlockRules.shouldBlockRequest("https://www.google-analytics.com/g/collect?v=2"))
        assertTrue(AdBlockRules.shouldBlockRequest("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js"))
    }

    @Test
    fun `ad and tracking paths are blocked on third party hosts`() {
        assertTrue(AdBlockRules.shouldBlockRequest("https://cdn.example.com/adserver/banner.png"))
        assertTrue(AdBlockRules.shouldBlockRequest("https://cdn.example.com/gtag/js?id=UA-1"))
        assertFalse(AdBlockRules.shouldBlockRequest("https://cdn.example.com/assets/app.js"))
    }

    @Test
    fun `regular page requests stay untouched`() {
        assertFalse(AdBlockRules.shouldBlockRequest("https://example.com/"))
        assertFalse(AdBlockRules.shouldBlockRequest("https://example.com/article/1?from=home"))
        assertFalse(AdBlockRules.shouldBlockRequest("https://example.com/style/main.css"))
    }
}
