package com.nanami.koishi.feature.tools.mini_apps.engine

import java.util.Locale

/**
 * 内置浏览器的 User-Agent 与视口处理
 */
object MiniAppUserAgent {

    const val DESKTOP_LAYOUT_WIDTH = 1024

    private const val MIN_DESKTOP_SCALE = 0.25f
    private const val DEFAULT_VIEW_WIDTH_CSS_PX = 412f
    private const val FALLBACK_CHROME_VERSION = "120"

    private val chromeVersionPattern = Regex("Chrome/(\\d+)")

    /**
     * 去掉 UA 中的 wv 标记，避免站点把内置浏览器判定为不支持的客户端
     */
    fun sanitize(defaultUserAgent: String): String = defaultUserAgent.replace("; wv", "")

    /**
     * 桌面版 UA 沿用设备 WebView 的 Chrome 内核版本，避免站点判定为过期浏览器
     */
    fun desktopUserAgent(mobileUserAgent: String): String {
        val version = chromeVersionPattern.find(mobileUserAgent)
            ?.groupValues
            ?.get(1)
            ?: FALLBACK_CHROME_VERSION
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$version.0.0.0 Safari/537.36"
    }

    /**
     * 按屏幕宽度算出让桌面布局刚好铺满的缩放比，供注入脚本使用
     */
    fun desktopScale(viewWidthCssPx: Float, width: Int = DESKTOP_LAYOUT_WIDTH): Float =
        (viewWidthCssPx / width).coerceIn(MIN_DESKTOP_SCALE, 1f)

    /**
     * 在页面内改写 viewport，把布局宽度撑到 [width] 并设置初始缩放
     */
    fun desktopViewportScript(
        viewWidthCssPx: Float = DEFAULT_VIEW_WIDTH_CSS_PX,
        width: Int = DESKTOP_LAYOUT_WIDTH
    ): String {
        val scale = String.format(Locale.US, "%.3f", desktopScale(viewWidthCssPx, width))
        return """
            |(function() {
            |    var meta = document.querySelector('meta[name="viewport"]');
            |    if (!meta) {
            |        if (!document.head) return;
            |        meta = document.createElement('meta');
            |        meta.setAttribute('name', 'viewport');
            |        document.head.appendChild(meta);
            |    }
            |    meta.setAttribute('content',
            |        'width=$width, initial-scale=$scale, minimum-scale=0.25, maximum-scale=5.0, user-scalable=yes');
            |})();
        """.trimMargin()
    }
}
