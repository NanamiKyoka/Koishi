package com.nanami.koishi.feature.tools.mini_apps.engine

import android.webkit.WebSettings
import androidx.webkit.UserAgentMetadata
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * WebView 的 Client Hints 品牌列表只有 Chromium，与 Chrome UA 一起发出时会被站点的风控判定成工具客户端，
 * 这里按当前 UA 对齐品牌、平台与设备形态，让内核以真实 Chrome 的身份对外
 */
object MiniAppClientHints {

    private const val GREASE_BRAND = "Not_A Brand"
    private const val GREASE_VERSION = "24"
    private const val DESKTOP_PLATFORM_VERSION = "10.0.0"

    private val chromeVersionPattern = Regex("Chrome/(\\d+)")

    data class Brand(val name: String, val version: String, val fullVersion: String)

    data class Profile(
        val brands: List<Brand>,
        val fullVersion: String,
        val platform: String,
        val platformVersion: String,
        val model: String,
        val mobile: Boolean,
        val formFactor: String
    )

    fun profile(
        userAgent: String,
        desktopMode: Boolean,
        deviceRelease: String,
        deviceModel: String
    ): Profile? {
        val version = chromeVersionPattern.find(userAgent)?.groupValues?.get(1) ?: return null
        val fullVersion = "$version.0.0.0"
        return Profile(
            brands = listOf(
                Brand(GREASE_BRAND, GREASE_VERSION, "$GREASE_VERSION.0.0.0"),
                Brand("Chromium", version, fullVersion),
                Brand("Google Chrome", version, fullVersion)
            ),
            fullVersion = fullVersion,
            platform = if (desktopMode) "Windows" else "Android",
            platformVersion = if (desktopMode) DESKTOP_PLATFORM_VERSION else deviceRelease,
            model = if (desktopMode) "" else deviceModel,
            mobile = !desktopMode,
            formFactor = if (desktopMode) {
                UserAgentMetadata.FORM_FACTOR_DESKTOP
            } else {
                UserAgentMetadata.FORM_FACTOR_MOBILE
            }
        )
    }

    /**
     * BrandVersion 要求品牌、主版本与完整版本三者都非空，因此统一由 Profile 生成
     */
    fun apply(settings: WebSettings, profile: Profile) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA)) return

        val formFactorsSupported =
            WebViewFeature.isFeatureSupported(WebViewFeature.USER_AGENT_METADATA_FORM_FACTORS)
        runCatching {
            WebSettingsCompat.setUserAgentMetadata(
                settings,
                profile.toUserAgentMetadata(formFactorsSupported)
            )
        }
    }
}

fun MiniAppClientHints.Profile.toUserAgentMetadata(
    formFactorsSupported: Boolean
): UserAgentMetadata {
    val builder = UserAgentMetadata.Builder()
        .setBrandVersionList(
            brands.map { brand ->
                UserAgentMetadata.BrandVersion.Builder()
                    .setBrand(brand.name)
                    .setMajorVersion(brand.version)
                    .setFullVersion(brand.fullVersion)
                    .build()
            }
        )
        .setFullVersion(fullVersion)
        .setPlatform(platform)
        .setPlatformVersion(platformVersion)
        .setModel(model)
        .setMobile(mobile)
        .setBitness(UserAgentMetadata.BITNESS_DEFAULT)

    if (formFactorsSupported) {
        builder.setFormFactors(listOf(formFactor))
    }
    return builder.build()
}
