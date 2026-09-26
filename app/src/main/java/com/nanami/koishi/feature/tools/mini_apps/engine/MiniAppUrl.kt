package com.nanami.koishi.feature.tools.mini_apps.engine

import java.util.Locale

/**
 * 网址规范化与主机解析，输入允许省略协议，最终统一升级为 https 以符合应用的明文流量策略
 */
object MiniAppUrl {

    private const val HTTPS = "https"
    private const val MAX_HOST_LENGTH = 253

    private val schemePattern = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*://")
    private val supportedSchemes = setOf("http", "https")
    private val domainPattern = Regex(
        "^(?=.{1,253}$)([\\p{L}\\p{N}]([\\p{L}\\p{N}\\-]{0,61}[\\p{L}\\p{N}])?\\.)+[\\p{L}]{2,}$"
    )
    private val ipv4Pattern = Regex("^\\d{1,3}(\\.\\d{1,3}){3}$")

    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val declaredScheme = schemePattern.find(trimmed)
            ?.value
            ?.removeSuffix("://")
            ?.lowercase(Locale.US)
        if (declaredScheme != null && declaredScheme !in supportedSchemes) return null

        val candidate = if (declaredScheme != null) trimmed else "$HTTPS://$trimmed"
        val authority = candidate.substringAfter("://", "")
        if (authority.isEmpty() || authority.startsWith("/")) return null

        val authorityPrefix = authority.substringBefore('/').substringBefore('?').substringBefore('#')
        val hostPort = authorityPrefix.substringAfterLast('@')
        if (hostPort.isEmpty()) return null

        val host = hostPort.substringBefore(':').lowercase(Locale.US)
        val port = hostPort.substringAfter(':', "")
        if (!isValidHost(host)) return null
        if (port.isNotEmpty() && (port.toIntOrNull() ?: 0) !in 1..65535) return null

        val suffix = authority.substring(authorityPrefix.length)
        val portSuffix = if (port.isEmpty() || port == "443") "" else ":$port"
        return "$HTTPS://$host$portSuffix$suffix"
    }

    fun isValidHost(host: String): Boolean {
        if (host.isEmpty() || host.length > MAX_HOST_LENGTH) return false
        if (host == "localhost") return true
        if (ipv4Pattern.matches(host)) return host.split('.').all { it.toInt() in 0..255 }
        return domainPattern.matches(host)
    }

    fun hostOf(url: String): String? {
        val authority = url.substringAfter("://", "")
        if (authority.isEmpty() || authority.startsWith("/")) return null
        val host = authority
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('@')
            .substringBefore(':')
            .lowercase(Locale.US)
        return host.ifEmpty { null }
    }

    /**
     * 去掉 www 前缀的主域名，用于同一站点下的数据清理
     */
    fun siteDomainOf(url: String): String? {
        val host = hostOf(url) ?: return null
        return if (host.startsWith("www.") && host.length > 4) host.drop(4) else host
    }

    fun faviconUrl(url: String): String? = hostOf(url)?.let { "$HTTPS://$it/favicon.ico" }

    /**
     * 判定网页回调的标题是否可用于替换占位名称，避免把原始网址或主机名写回条目
     */
    fun isMeaningfulTitle(title: String, url: String): Boolean {
        val value = title.trim()
        if (value.isEmpty()) return false
        if (schemePattern.containsMatchIn(value)) return false
        return !value.equals(hostOf(url), ignoreCase = true)
    }
}
