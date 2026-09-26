package com.nanami.koishi.feature.tools.mini_apps.engine

import java.util.Locale

/**
 * 常见第三方广告与追踪拦截规则，仅依赖纯字符串匹配以便在 JVM 侧直接测试
 */
object AdBlockRules {

    private val blockedHostSuffixes = setOf(
        "2mdn.net",
        "360yield.com",
        "adcolony.com",
        "addthis.com",
        "adform.net",
        "admaster.com.cn",
        "adnxs.com",
        "adsafeprotected.com",
        "adsame.com",
        "adsrvr.org",
        "adview.cn",
        "adjust.com",
        "alimama.cn",
        "allyes.com",
        "amazon-adsystem.com",
        "applovin.com",
        "appsflyer.com",
        "bluekai.com",
        "branch.io",
        "casalemedia.com",
        "chartboost.com",
        "cnzz.com",
        "contextweb.com",
        "cpro.baidu.com",
        "criteo.com",
        "criteo.net",
        "demdex.net",
        "doubleclick.net",
        "dup.baidu.com",
        "e.qq.com",
        "exelator.com",
        "flurry.com",
        "gdt.qq.com",
        "google-analytics.com",
        "googleadservices.com",
        "googlesyndication.com",
        "googletagmanager.com",
        "googletagservices.com",
        "growingio.com",
        "hm.baidu.com",
        "imrworldwide.com",
        "inmobi.com",
        "ipinyou.com",
        "kochava.com",
        "krxd.net",
        "mathtag.com",
        "media.net",
        "miaozhen.com",
        "mixpanel.com",
        "mmstat.com",
        "moatads.com",
        "mopub.com",
        "oceanengine.com",
        "openx.net",
        "outbrain.com",
        "pangolin-sdk-toutiao.com",
        "pos.baidu.com",
        "pubmatic.com",
        "quantserve.com",
        "quantcount.com",
        "rubiconproject.com",
        "scorecardresearch.com",
        "sharethrough.com",
        "smartadserver.com",
        "taboola.com",
        "talkingdata.com",
        "talkingdata.net",
        "tanx.com",
        "teads.tv",
        "tremorhub.com",
        "umeng.com",
        "umengcloud.com",
        "unityads.unity3d.com",
        "vungle.com",
        "yieldmo.com",
        "zqtk.net"
    )

    private val blockedPathFragments = setOf(
        "/ad_banner",
        "/adbanner",
        "/adclick",
        "/ad_click",
        "/adserver",
        "/adsense",
        "/advert/",
        "/advertising/",
        "/analytics.js",
        "/beacon?",
        "/collect?",
        "/gtag/js",
        "/impression?",
        "/pagead",
        "/pixel.gif",
        "/popunder",
        "/track/",
        "/track?",
        "/tracker",
        "v1/track"
    )

    /**
     * 命中广告或追踪域名，用于拦截跳转与子资源请求
     */
    fun isBlockedHost(host: String?): Boolean {
        val normalized = host?.lowercase(Locale.US)?.trim()?.trim('.') ?: return false
        if (normalized.isEmpty()) return false
        return blockedHostSuffixes.any { normalized == it || normalized.endsWith(".$it") }
    }

    /**
     * 子资源请求拦截，除域名规则外再补充常见的广告与埋点路径特征
     */
    fun shouldBlockRequest(url: String): Boolean {
        val lower = url.lowercase(Locale.US)
        val authority = lower.substringAfter("://", "")
        if (authority.isEmpty()) return false

        val host = authority
            .substringBefore('/')
            .substringBefore('?')
            .substringAfterLast('@')
            .substringBefore(':')
        if (isBlockedHost(host)) return true

        val path = "/" + authority.substringAfter('/', "").substringBefore('#')
        return blockedPathFragments.any { path.contains(it) }
    }
}
