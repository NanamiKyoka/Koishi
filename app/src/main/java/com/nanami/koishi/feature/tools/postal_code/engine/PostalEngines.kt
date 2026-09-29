package com.nanami.koishi.feature.tools.postal_code.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 海外邮编查询走 Zippopotam 公开接口，中国大陆邮编由 [PostalDataset] 本地解析。
 * 数据来源：https://www.zippopotam.us
 */
object PostalEngines {

    private const val ZIPPOPOTAM_ENDPOINT = "https://api.zippopotam.us"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    suspend fun fetchFromZippopotam(
        countryCode: String,
        postalCode: String
    ): List<PostalRecord> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$ZIPPOPOTAM_ENDPOINT/${countryCode.lowercase()}/$postalCode")
            .header("Accept", "application/json")
            .get()
            .build()

        val response = try {
            httpClient.newCall(request).execute()
        } catch (_: Exception) {
            throw PostalException.Network()
        }

        response.use { res ->
            val bodyText = res.body?.string().orEmpty()
            if (res.code == 404) throw PostalException.NotFound()
            if (!res.isSuccessful) throw PostalException.ServerError()

            parseZippopotamBody(bodyText, postalCode)
        }
    }

    private fun parseZippopotamBody(bodyText: String, requestedCode: String): List<PostalRecord> {
        val root = try {
            json.parseToJsonElement(bodyText).jsonObject
        } catch (_: Exception) {
            throw PostalException.Parse()
        }

        val countryName = root.stringOrEmpty("country")
        val postCode = root.stringOrEmpty("post code").ifBlank { requestedCode }
        val places = root["places"]?.jsonArrayOrEmpty() ?: return emptyList()

        return places.mapNotNull { element ->
            val place = element.jsonObjectOrNull() ?: return@mapNotNull null
            val placeName = place.stringOrEmpty("place name")
            if (placeName.isBlank()) return@mapNotNull null

            PostalRecord(
                postalCode = postCode,
                countryName = countryName,
                province = place.stringOrEmpty("state"),
                city = placeName,
                latitude = place.stringOrEmpty("latitude"),
                longitude = place.stringOrEmpty("longitude")
            )
        }
    }
}

private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement.jsonArrayOrEmpty(): JsonArray = this as? JsonArray ?: JsonArray(emptyList())

private fun JsonObject.stringOrEmpty(key: String): String =
    runCatching { this[key]?.jsonPrimitive?.contentOrNull.orEmpty().trim() }.getOrDefault("")
