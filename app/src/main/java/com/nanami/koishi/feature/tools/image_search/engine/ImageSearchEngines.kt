package com.nanami.koishi.feature.tools.image_search.engine

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.engine.parser.Ascii2dCloudflareException
import com.nanami.koishi.feature.tools.image_search.engine.parser.Ascii2dParser
import com.nanami.koishi.feature.tools.image_search.engine.parser.SauceNaoException
import com.nanami.koishi.feature.tools.image_search.engine.parser.SauceNaoParser
import com.nanami.koishi.feature.tools.image_search.engine.parser.TraceMoeParser
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchState
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchStatus
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import com.nanami.koishi.feature.tools.image_search.model.SearchResultItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object ImageSearchEngines {

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * SauceNAO 检索引擎
     */
    suspend fun searchSauceNao(imageBytes: ByteArray, apiKey: String?): EngineSearchState = withContext(Dispatchers.IO) {
        val fallbackWebUrl = "https://saucenao.com/"
        try {
            val multipartBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("db", "999")
                .addFormDataPart("output_type", "2")
                .addFormDataPart("numres", "12")
                .addFormDataPart(
                    "file",
                    "search.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )

            if (!apiKey.isNullOrBlank()) {
                multipartBuilder.addFormDataPart("api_key", apiKey.trim())
            }

            val request = Request.Builder()
                .url("https://saucenao.com/search.php")
                .header("User-Agent", USER_AGENT)
                .post(multipartBuilder.build())
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext EngineSearchState(
                    engine = SearchEngineEnum.SAUCENAO,
                    status = EngineSearchStatus.FALLBACK_REQUIRED,
                    errorMessageRes = R.string.image_search_error_server_resp,
                    fallbackUrl = fallbackWebUrl
                )
            }

            val results = SauceNaoParser.parse(responseBody)
            if (results.isEmpty()) {
                EngineSearchState(
                    engine = SearchEngineEnum.SAUCENAO,
                    status = EngineSearchStatus.EMPTY,
                    fallbackUrl = fallbackWebUrl
                )
            } else {
                EngineSearchState(
                    engine = SearchEngineEnum.SAUCENAO,
                    status = EngineSearchStatus.SUCCESS,
                    results = results,
                    fallbackUrl = fallbackWebUrl
                )
            }
        } catch (e: SauceNaoException) {
            val resId = when (e.statusCode) {
                -1 -> R.string.image_search_error_rate_limit
                -2 -> R.string.image_search_error_invalid_key
                -3 -> R.string.image_search_error_quota_exceeded
                else -> R.string.image_search_error_server_resp
            }
            EngineSearchState(
                engine = SearchEngineEnum.SAUCENAO,
                status = EngineSearchStatus.FALLBACK_REQUIRED,
                errorMessageRes = resId,
                fallbackUrl = fallbackWebUrl
            )
        } catch (e: Exception) {
            val isNetworkErr = e is SocketException || e is SocketTimeoutException || e is UnknownHostException || e is IOException
            EngineSearchState(
                engine = SearchEngineEnum.SAUCENAO,
                status = EngineSearchStatus.FALLBACK_REQUIRED,
                errorMessageRes = if (isNetworkErr) R.string.image_search_error_network else R.string.image_search_error_server_resp,
                errorMessage = e.localizedMessage,
                fallbackUrl = fallbackWebUrl
            )
        }
    }

    /**
     * trace.moe 检索引擎
     */
    suspend fun searchTraceMoe(imageBytes: ByteArray): EngineSearchState = withContext(Dispatchers.IO) {
        val fallbackWebUrl = "https://trace.moe/"
        try {
            val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())
            val request = Request.Builder()
                .url("https://api.trace.moe/search?anilistInfo")
                .header("User-Agent", USER_AGENT)
                .header("Content-Type", "image/jpeg")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext EngineSearchState(
                    engine = SearchEngineEnum.TRACE_MOE,
                    status = EngineSearchStatus.ERROR,
                    errorMessageRes = R.string.image_search_error_server_resp,
                    fallbackUrl = fallbackWebUrl
                )
            }

            val results = TraceMoeParser.parse(responseBody)
            if (results.isEmpty()) {
                EngineSearchState(
                    engine = SearchEngineEnum.TRACE_MOE,
                    status = EngineSearchStatus.EMPTY,
                    fallbackUrl = fallbackWebUrl
                )
            } else {
                EngineSearchState(
                    engine = SearchEngineEnum.TRACE_MOE,
                    status = EngineSearchStatus.SUCCESS,
                    results = results,
                    fallbackUrl = fallbackWebUrl
                )
            }
        } catch (e: Exception) {
            val isNetworkErr = e is SocketException || e is SocketTimeoutException || e is UnknownHostException || e is IOException
            EngineSearchState(
                engine = SearchEngineEnum.TRACE_MOE,
                status = EngineSearchStatus.ERROR,
                errorMessageRes = if (isNetworkErr) R.string.image_search_error_network else null,
                errorMessage = if (!isNetworkErr) e.localizedMessage else null,
                fallbackUrl = fallbackWebUrl
            )
        }
    }

    /**
     * ascii2d 检索引擎（抓取 + 降级处理）
     */
    suspend fun searchAscii2d(imageBytes: ByteArray): EngineSearchState = withContext(Dispatchers.IO) {
        val defaultFallbackUrl = "https://ascii2d.net/"
        try {
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "upload.jpg",
                    imageBytes.toRequestBody("image/jpeg".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://ascii2d.net/search/file")
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Origin", "https://ascii2d.net")
                .header("Referer", "https://ascii2d.net/")
                .post(multipart)
                .build()

            val response = httpClient.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            val html = response.body?.string().orEmpty()

            if (response.code == 403 || Ascii2dParser.isCloudflareBlocked(html)) {
                return@withContext EngineSearchState(
                    engine = SearchEngineEnum.ASCII2D,
                    status = EngineSearchStatus.FALLBACK_REQUIRED,
                    errorMessageRes = R.string.image_search_error_cloudflare,
                    fallbackUrl = if (finalUrl.contains("/search/")) finalUrl else defaultFallbackUrl
                )
            }

            if (!response.isSuccessful) {
                return@withContext EngineSearchState(
                    engine = SearchEngineEnum.ASCII2D,
                    status = EngineSearchStatus.FALLBACK_REQUIRED,
                    errorMessageRes = R.string.image_search_error_server_resp,
                    fallbackUrl = defaultFallbackUrl
                )
            }

            val results = Ascii2dParser.parse(html, finalUrl)
            if (results.isEmpty()) {
                EngineSearchState(
                    engine = SearchEngineEnum.ASCII2D,
                    status = EngineSearchStatus.EMPTY,
                    fallbackUrl = finalUrl
                )
            } else {
                EngineSearchState(
                    engine = SearchEngineEnum.ASCII2D,
                    status = EngineSearchStatus.SUCCESS,
                    results = results,
                    fallbackUrl = finalUrl
                )
            }
        } catch (_: Ascii2dCloudflareException) {
            EngineSearchState(
                engine = SearchEngineEnum.ASCII2D,
                status = EngineSearchStatus.FALLBACK_REQUIRED,
                errorMessageRes = R.string.image_search_error_cloudflare,
                fallbackUrl = defaultFallbackUrl
            )
        } catch (e: Exception) {
            val isNetworkErr = e is SocketException || e is SocketTimeoutException || e is UnknownHostException || e is IOException
            EngineSearchState(
                engine = SearchEngineEnum.ASCII2D,
                status = EngineSearchStatus.FALLBACK_REQUIRED,
                errorMessageRes = if (isNetworkErr) R.string.image_search_error_network else null,
                errorMessage = if (!isNetworkErr) e.localizedMessage else null,
                fallbackUrl = defaultFallbackUrl
            )
        }
    }

    /**
     * Google Lens 检索引擎
     */
    fun createGoogleLensState(): EngineSearchState {
        val lensUrl = "https://lens.google.com/"
        return EngineSearchState(
            engine = SearchEngineEnum.GOOGLE_LENS,
            status = EngineSearchStatus.SUCCESS,
            fallbackUrl = lensUrl
        )
    }
}
