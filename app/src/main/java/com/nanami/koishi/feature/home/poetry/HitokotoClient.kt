package com.nanami.koishi.feature.home.poetry

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object HitokotoClient {

    private const val ENDPOINT = "https://v1.hitokoto.cn/?encode=json&charset=utf-8"

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .build()
    }

    suspend fun fetch(): Hitokoto = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(ENDPOINT).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Hitokoto responded with ${response.code}")
            }
            parse(response.body?.string().orEmpty())
        }
    }

    private fun parse(body: String): Hitokoto {
        val root = json.parseToJsonElement(body).jsonObject
        val text = root.stringOrEmpty("hitokoto")
        if (text.isEmpty()) throw IOException("Hitokoto response carries no sentence")

        return Hitokoto(
            uuid = root.stringOrEmpty("uuid"),
            text = text,
            source = root.stringOrEmpty("from"),
            author = root.stringOrEmpty("from_who")
        )
    }

    private fun JsonObject.stringOrEmpty(key: String): String =
        (this[key] as? JsonPrimitive)?.contentOrNull.orEmpty().trim()
}
