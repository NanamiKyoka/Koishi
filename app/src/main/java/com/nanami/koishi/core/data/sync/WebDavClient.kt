package com.nanami.koishi.core.data.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

open class WebDavException(message: String, cause: Throwable? = null) : IOException(message, cause)
class WebDavAuthException(message: String = "Authentication failed") : WebDavException(message)
class WebDavFileNotFoundException(message: String = "Backup file not found") : WebDavException(message)
class WebDavServerException(val code: Int, message: String) : WebDavException("HTTP $code: $message")

class WebDavClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(config: WebDavConfig) = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(config.serverUrl)
        val credential = Credentials.basic(config.username, config.password)

        val propfindRequest = Request.Builder()
            .url(normalizedUrl)
            .header("Authorization", credential)
            .header("Depth", "0")
            .method("PROPFIND", null)
            .build()

        try {
            httpClient.newCall(propfindRequest).execute().use { response ->
                when {
                    response.isSuccessful || response.code == 207 || response.code == 405 -> return@withContext
                    response.code == 401 -> throw WebDavAuthException()
                    response.code == 403 -> throw WebDavException("Forbidden")
                    response.code == 404 -> throw WebDavException("Server URL not found")
                    else -> throw WebDavServerException(response.code, response.message)
                }
            }
        } catch (e: Exception) {
            if (e is WebDavAuthException) throw e
            val getRequest = Request.Builder()
                .url(normalizedUrl)
                .header("Authorization", credential)
                .head()
                .build()

            httpClient.newCall(getRequest).execute().use { response ->
                when {
                    response.isSuccessful || response.code == 405 -> return@withContext
                    response.code == 401 -> throw WebDavAuthException()
                    response.code == 403 -> throw WebDavException("Forbidden")
                    response.code == 404 -> throw WebDavException("Server URL not found")
                    else -> throw WebDavServerException(response.code, response.message)
                }
            }
        }
    }

    suspend fun uploadBackup(config: WebDavConfig, jsonContent: String) = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(config.serverUrl)
        val credential = Credentials.basic(config.username, config.password)
        val folderUrl = "$normalizedUrl/Koishi"
        val fileUrl = "$folderUrl/koishi_backup.json"

        ensureRemoteDirectory(folderUrl, credential)

        val body = jsonContent.toRequestBody(jsonMediaType)
        val putRequest = Request.Builder()
            .url(fileUrl)
            .header("Authorization", credential)
            .put(body)
            .build()

        httpClient.newCall(putRequest).execute().use { response ->
            when {
                response.isSuccessful || response.code == 201 || response.code == 204 -> return@withContext
                response.code == 401 -> throw WebDavAuthException()
                else -> throw WebDavServerException(response.code, response.message)
            }
        }
    }

    suspend fun downloadBackup(config: WebDavConfig): String = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(config.serverUrl)
        val credential = Credentials.basic(config.username, config.password)
        val fileUrl = "$normalizedUrl/Koishi/koishi_backup.json"

        val request = Request.Builder()
            .url(fileUrl)
            .header("Authorization", credential)
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> throw WebDavFileNotFoundException()
                response.code == 401 -> throw WebDavAuthException()
                !response.isSuccessful -> throw WebDavServerException(response.code, response.message)
                else -> response.body?.string().orEmpty()
            }
        }
    }

    private fun ensureRemoteDirectory(folderUrl: String, credential: String) {
        val mkcolRequest = Request.Builder()
            .url(folderUrl)
            .header("Authorization", credential)
            .method("MKCOL", null)
            .build()

        try {
            httpClient.newCall(mkcolRequest).execute().use { response ->
                if (response.code == 401) throw WebDavAuthException()
            }
        } catch (e: Exception) {
            if (e is WebDavAuthException) throw e
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        val withProtocol = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }
        return withProtocol.trimEnd('/')
    }
}
