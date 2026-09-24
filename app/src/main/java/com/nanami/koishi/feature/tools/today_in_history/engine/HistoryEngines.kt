package com.nanami.koishi.feature.tools.today_in_history.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object HistoryEngines {

    private const val SHOW_API_ENDPOINT = "https://route.showapi.com/119-42"
    private const val SHOW_API_ACCOUNT_URL = "https://www.showapi.com/"
    private const val XXAPI_ENDPOINT = "https://v2.xxapi.cn/api/history"

    const val SHOW_API_HOME = SHOW_API_ACCOUNT_URL

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    /**
     * ShowAPI 返回的图片地址为 http 明文协议，Android 9+ 默认拦截明文流量，
     * 而同一资源在 https 下同样可用，因此统一升级协议以免图片静默加载失败。
     */
    private fun normalizeImageUrl(raw: String): String {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://") -> "https://" + trimmed.removePrefix("http://")
            else -> trimmed
        }
    }

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val datePattern = Regex("^\\s*(\\d{1,4})年(\\d{1,2})月(\\d{1,2})日\\s*(.*)$")

    suspend fun fetchFromShowApi(
        appKey: String,
        month: Int,
        day: Int
    ): List<HistoryEvent> = withContext(Dispatchers.IO) {
        require(appKey.isNotBlank()) { "appKey is blank" }

        val formBody = FormBody.Builder()
            .add("date", "%02d%02d".format(month, day))
            .add("needContent", "1")
            .build()

        val request = Request.Builder()
            .url("$SHOW_API_ENDPOINT?appKey=$appKey")
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .post(formBody)
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw toHistoryException(e)
        }

        response.use { res ->
            val bodyText = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw classifyHttpError(res.code, bodyText)
            }
            parseShowApiBody(bodyText, month, day)
        }
    }

    private fun parseShowApiBody(bodyText: String, month: Int, day: Int): List<HistoryEvent> {
        val root = try {
            json.parseToJsonElement(bodyText).jsonObject
        } catch (e: Exception) {
            throw HistoryException.Parse()
        }

        val resCode = root["showapi_res_code"]?.jsonPrimitive?.intOrNull ?: 0
        if (resCode != 0) {
            val errorText = root["showapi_res_error"]?.jsonPrimitive?.contentOrNull.orEmpty()
            throw classifyShowApiError(resCode, errorText)
        }

        val body = root["showapi_res_body"]?.jsonObject

        val retCode = body?.get("ret_code")?.jsonPrimitive?.intOrNull
        if (retCode != null && retCode != 0) {
            throw classifyShowApiError(retCode, "")
        }

        val list = body?.get("list")?.let { it as? JsonArray } ?: return emptyList()

        return list.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val title = item["title"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            if (title.isEmpty()) return@mapNotNull null

            HistoryEvent(
                title = title,
                year = item["year"]?.jsonPrimitive?.contentOrNull.orEmpty().toIntOrNull() ?: 0,
                month = item["month"]?.jsonPrimitive?.intOrNull ?: month,
                day = item["day"]?.jsonPrimitive?.intOrNull ?: day,
                content = item["content"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
                imageUrl = normalizeImageUrl(item["img"]?.jsonPrimitive?.contentOrNull.orEmpty())
            )
        }
    }

    suspend fun fetchFromXXapi(month: Int, day: Int): List<HistoryEvent> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(XXAPI_ENDPOINT)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (e: Exception) {
            throw toHistoryException(e)
        }

        response.use { res ->
            val bodyText = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw HistoryException.ServerError(res.code)
            }
            parseXXapiBody(bodyText, month, day)
        }
    }

    private fun parseXXapiBody(bodyText: String, month: Int, day: Int): List<HistoryEvent> {
        val root = try {
            json.parseToJsonElement(bodyText).jsonObject
        } catch (e: Exception) {
            throw HistoryException.Parse()
        }

        val code = root["code"]?.jsonPrimitive?.intOrNull ?: 0
        if (code != 200 && code != 0) {
            throw HistoryException.ServerError(code)
        }

        val list = root["data"] as? JsonArray ?: return emptyList()

        return list.mapNotNull { element ->
            val raw = element.jsonPrimitive.contentOrNull.orEmpty().trim()
            if (raw.isEmpty()) return@mapNotNull null
            parsePlainTextEvent(raw, month, day)
        }
    }

    private fun parsePlainTextEvent(raw: String, month: Int, day: Int): HistoryEvent? {
        val match = datePattern.find(raw) ?: return HistoryEvent(
            title = raw,
            year = 0,
            month = month,
            day = day
        )

        val (yearText, monthText, dayText, titleText) = match.destructured
        return HistoryEvent(
            title = titleText.trim().ifEmpty { raw },
            year = yearText.toIntOrNull() ?: 0,
            month = monthText.toIntOrNull() ?: month,
            day = dayText.toIntOrNull() ?: day
        )
    }

    private fun toHistoryException(e: Exception): HistoryException = when (e) {
        is SocketException, is SocketTimeoutException, is UnknownHostException, is IOException ->
            HistoryException.Network()
        else -> HistoryException.Network()
    }

    private fun classifyHttpError(statusCode: Int, bodyText: String): HistoryException {
        val text = bodyText.lowercase()
        return when {
            statusCode == 401 || statusCode == 403 -> HistoryException.InvalidApiKey()
            statusCode == 429 || text.contains("quota") || text.contains("次数") ->
                HistoryException.QuotaExceeded()
            else -> HistoryException.ServerError(statusCode)
        }
    }

    private fun classifyShowApiError(resCode: Int, errorText: String): HistoryException {
        val text = errorText.lowercase()
        return when {
            resCode == -1004 || resCode == -1003 ->
                HistoryException.InvalidApiKey()
            resCode == -1006 || resCode == -1007 ->
                HistoryException.QuotaExceeded()
            text.contains("appkey") || text.contains("app key") || text.contains("密钥") ->
                HistoryException.InvalidApiKey()
            text.contains("次数") || text.contains("quota") || text.contains("limit") ||
                    text.contains("余额") || text.contains("积分") ->
                HistoryException.QuotaExceeded()
            else -> HistoryException.ServerError(resCode)
        }
    }
}
