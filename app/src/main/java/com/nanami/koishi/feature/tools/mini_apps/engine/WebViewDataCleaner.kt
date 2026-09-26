package com.nanami.koishi.feature.tools.mini_apps.engine

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * WebView 浏览数据清理：运行期数据走官方 API，缓存目录再做一次文件级兜底
 */
object WebViewDataCleaner {

    private const val WEB_VIEW_DATA_DIR = "app_webview"
    private const val ORIGIN_QUERY_TIMEOUT_MS = 1500L

    private val cacheDirectoryNames = setOf(
        "Cache",
        "Code Cache",
        "GPUCache",
        "GrShaderCache",
        "Service Worker",
        "shader_cache"
    )

    suspend fun clearAll(context: Context, webView: WebView? = null) {
        withContext(Dispatchers.Main) {
            runCatching {
                webView?.stopLoading()
                webView?.clearHistory()
                webView?.clearCache(true)
            }
            // 列表页没有存活的实例，临时建一个刷掉进程内共享的 HTTP 缓存
            runCatching {
                if (webView == null) {
                    WebView(context).apply { clearCache(true) }.destroy()
                }
            }
            runCatching {
                CookieManager.getInstance().apply {
                    removeAllCookies(null)
                    flush()
                }
            }
            runCatching { WebStorage.getInstance().deleteAllData() }
            runCatching { WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword() }
        }
        purgeCacheFiles(context)
    }

    suspend fun clearForSite(context: Context, webView: WebView?, url: String) {
        val host = MiniAppUrl.hostOf(url) ?: return
        val domain = MiniAppUrl.siteDomainOf(url) ?: host

        withContext(Dispatchers.Main) {
            runCatching { removeSiteCookies(host, domain) }
            runCatching { removeSiteStorage(host, domain) }
            runCatching { webView?.clearCache(true) }
        }
        purgeCacheFiles(context)
    }

    private fun removeSiteCookies(host: String, domain: String) {
        val manager = CookieManager.getInstance()
        (listOf(host, domain) + if (host != domain) listOf("www.$domain") else emptyList())
            .distinct()
            .forEach { target ->
                val origin = "https://$target/"
                val cookie = runCatching { manager.getCookie(origin) }.getOrNull() ?: return@forEach
                cookie.split(';').forEach { pair ->
                    val name = pair.substringBefore('=').trim()
                    if (name.isNotEmpty()) {
                        manager.setCookie(origin, "$name=; Max-Age=0; Path=/")
                    }
                }
            }
        manager.flush()
    }

    private suspend fun removeSiteStorage(host: String, domain: String) {
        val storage = WebStorage.getInstance()
        val origins = withTimeoutOrNull(ORIGIN_QUERY_TIMEOUT_MS) {
            suspendCoroutine<List<String>> { continuation ->
                storage.getOrigins { originMap ->
                    continuation.resume(originMap.keys.filterIsInstance<String>())
                }
            }
        } ?: return

        origins.forEach { origin ->
            val originHost = MiniAppUrl.hostOf(origin) ?: return@forEach
            val matched = originHost == host ||
                    originHost == domain ||
                    originHost.endsWith(".$domain")
            if (matched) {
                runCatching { storage.deleteOrigin(origin) }
            }
        }
    }

    private suspend fun purgeCacheFiles(context: Context) = withContext(Dispatchers.IO) {
        runCatching {
            context.cacheDir?.listFiles()
                ?.filter { it.isDirectory && it.name.contains("webview", ignoreCase = true) }
                ?.forEach { it.deleteRecursively() }
        }
        runCatching {
            val dataRoot = context.filesDir?.parentFile ?: return@runCatching
            File(dataRoot, WEB_VIEW_DATA_DIR).listFiles()
                ?.filter { it.isDirectory }
                ?.forEach { profile ->
                    profile.listFiles()
                        ?.filter { it.name in cacheDirectoryNames }
                        ?.forEach { it.deleteRecursively() }
                }
        }
    }
}
