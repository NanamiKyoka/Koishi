package com.nanami.koishi.feature.tools.mini_apps.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.getSystemService
import java.net.URLDecoder
import java.util.Locale

/**
 * 网页下载交给系统下载器落到公共下载目录，沿用页面的 Cookie 与 UA 以便取到需要登录的资源
 */
object MiniAppDownloader {

    private val dispositionPattern = Regex("filename\\*?=(?:UTF-8'')?([^;]+)", RegexOption.IGNORE_CASE)
    private val invalidNameChars = Regex("[\\\\/:*?\"<>|\\p{Cntrl}]")

    private val mimeExtensions = mapOf(
        "application/pdf" to "pdf",
        "application/zip" to "zip",
        "application/vnd.android.package-archive" to "apk",
        "image/jpeg" to "jpg",
        "image/png" to "png",
        "image/gif" to "gif",
        "image/webp" to "webp",
        "text/plain" to "txt",
        "video/mp4" to "mp4",
        "audio/mpeg" to "mp3"
    )

    fun isHttpUrl(url: String): Boolean {
        val scheme = url.substringBefore(':', "").lowercase(Locale.US)
        return scheme == "http" || scheme == "https"
    }

    /**
     * 按扩展名反推类型，供长按图片下载时补上准确的 MIME
     */
    fun mimeTypeFor(url: String): String? {
        val extension = url.substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('.', "")
            .lowercase(Locale.US)
        if (extension.isEmpty()) return null
        return mimeExtensions.entries.firstOrNull { it.value == extension }?.key
    }

    /**
     * 依次尝试响应头给出的文件名、网址末段，最后回退到按类型生成的默认名
     */
    fun fileName(url: String, contentDisposition: String?, mimeType: String?): String {
        contentDisposition
            ?.let { dispositionPattern.find(it)?.groupValues?.get(1) }
            ?.trim()
            ?.trim('"')
            ?.takeIf { it.isNotBlank() }
            ?.let { return sanitize(decode(it)) }

        val fromUrl = url.substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('/')
            .takeIf { it.isNotBlank() && it.contains('.') }
        if (fromUrl != null) return sanitize(decode(fromUrl))

        val extension = mimeExtensions[mimeType?.lowercase(Locale.US)] ?: "bin"
        return "download_${System.currentTimeMillis()}.$extension"
    }

    fun enqueue(
        context: Context,
        url: String,
        userAgent: String?,
        fileName: String,
        mimeType: String?,
        cookie: String?
    ): Boolean {
        val request = DownloadManager.Request(Uri.parse(url))
            .setMimeType(mimeType?.takeIf { it.isNotBlank() } ?: "application/octet-stream")
            .setTitle(fileName)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

        userAgent?.takeIf { it.isNotBlank() }?.let { request.addRequestHeader("User-Agent", it) }
        cookie?.takeIf { it.isNotBlank() }?.let { request.addRequestHeader("Cookie", it) }

        val manager = context.getSystemService<DownloadManager>() ?: return false
        return runCatching { manager.enqueue(request) }.isSuccess
    }

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, "UTF-8")
    }.getOrDefault(value)

    private fun sanitize(name: String): String =
        name.replace(invalidNameChars, "_").trim().take(MAX_NAME_LENGTH).ifEmpty { "download.bin" }

    private const val MAX_NAME_LENGTH = 120
}
