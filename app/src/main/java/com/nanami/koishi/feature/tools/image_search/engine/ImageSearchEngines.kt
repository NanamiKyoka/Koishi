package com.nanami.koishi.feature.tools.image_search.engine

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.engine.parser.SauceNaoException
import com.nanami.koishi.feature.tools.image_search.engine.parser.SauceNaoParser
import com.nanami.koishi.feature.tools.image_search.engine.parser.TraceMoeParser
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchState
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchStatus
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
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

    private const val SAUCENAO_HOME_URL = "https://saucenao.com/"
    private const val TRACE_MOE_HOME_URL = "https://trace.moe/"
    private const val GOOGLE_LENS_HOME_URL = "https://lens.google.com/"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private fun isNetworkError(e: Exception): Boolean =
        e is SocketException || e is SocketTimeoutException || e is UnknownHostException || e is IOException

    /**
     * SauceNAO 检索引擎。
     *
     * 未配置 API Key 时官方匿名账号不允许 API 调用（返回 status -1），
     * 因此这里直接判定为不可用，不发起请求，仅提供网页版降级入口。
     */
    suspend fun searchSauceNao(imageBytes: ByteArray, apiKey: String?): EngineSearchState {
        val trimmedKey = apiKey?.trim().orEmpty()
        if (trimmedKey.isEmpty()) {
            return EngineSearchState(
                engine = SearchEngineEnum.SAUCENAO,
                status = EngineSearchStatus.CONFIG_REQUIRED,
                errorMessageRes = R.string.image_search_saucenao_requires_key,
                fallbackUrl = SAUCENAO_HOME_URL
            )
        }

        return withContext(Dispatchers.IO) {
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
                    .addFormDataPart("api_key", trimmedKey)

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
                        fallbackUrl = SAUCENAO_HOME_URL
                    )
                }

                val results = SauceNaoParser.parse(responseBody)
                if (results.isEmpty()) {
                    EngineSearchState(
                        engine = SearchEngineEnum.SAUCENAO,
                        status = EngineSearchStatus.EMPTY,
                        fallbackUrl = SAUCENAO_HOME_URL
                    )
                } else {
                    EngineSearchState(
                        engine = SearchEngineEnum.SAUCENAO,
                        status = EngineSearchStatus.SUCCESS,
                        results = results,
                        fallbackUrl = SAUCENAO_HOME_URL
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
                    fallbackUrl = SAUCENAO_HOME_URL
                )
            } catch (e: Exception) {
                val networkError = isNetworkError(e)
                EngineSearchState(
                    engine = SearchEngineEnum.SAUCENAO,
                    status = EngineSearchStatus.FALLBACK_REQUIRED,
                    errorMessageRes = if (networkError) R.string.image_search_error_network else R.string.image_search_error_server_resp,
                    errorMessage = if (networkError) null else e.localizedMessage,
                    fallbackUrl = SAUCENAO_HOME_URL
                )
            }
        }
    }

    /**
     * trace.moe 检索引擎
     */
    suspend fun searchTraceMoe(imageBytes: ByteArray): EngineSearchState = withContext(Dispatchers.IO) {
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
                    fallbackUrl = TRACE_MOE_HOME_URL
                )
            }

            val results = TraceMoeParser.parse(responseBody)
            if (results.isEmpty()) {
                EngineSearchState(
                    engine = SearchEngineEnum.TRACE_MOE,
                    status = EngineSearchStatus.EMPTY,
                    fallbackUrl = TRACE_MOE_HOME_URL
                )
            } else {
                EngineSearchState(
                    engine = SearchEngineEnum.TRACE_MOE,
                    status = EngineSearchStatus.SUCCESS,
                    results = results,
                    fallbackUrl = TRACE_MOE_HOME_URL
                )
            }
        } catch (e: Exception) {
            EngineSearchState(
                engine = SearchEngineEnum.TRACE_MOE,
                status = EngineSearchStatus.ERROR,
                errorMessageRes = if (isNetworkError(e)) R.string.image_search_error_network else null,
                errorMessage = if (isNetworkError(e)) null else e.localizedMessage,
                fallbackUrl = TRACE_MOE_HOME_URL
            )
        }
    }

    /**
     * Google Lens 检索引擎
     */
    fun createGoogleLensState(): EngineSearchState = EngineSearchState(
        engine = SearchEngineEnum.GOOGLE_LENS,
        status = EngineSearchStatus.SUCCESS,
        fallbackUrl = GOOGLE_LENS_HOME_URL
    )
}
