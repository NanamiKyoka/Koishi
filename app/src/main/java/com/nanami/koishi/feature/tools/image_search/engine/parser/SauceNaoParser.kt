package com.nanami.koishi.feature.tools.image_search.engine.parser

import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.image_search.model.SearchResultItem
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class SauceNaoException(val statusCode: Int, message: String) : Exception(message)

object SauceNaoParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * 安全提取 JsonElement 为 String，如果为 Array 则拼接，遇到 Object/Null 不崩溃
     */
    fun JsonElement?.asSafeString(): String? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.contentOrNull
            is JsonArray -> this.mapNotNull { it.asSafeString() }.filter { it.isNotBlank() }.joinToString(", ").ifBlank { null }
            else -> null
        }
    }

    private fun JsonElement?.asSafeInt(): Int? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.intOrNull
            else -> null
        }
    }

    private fun JsonElement?.asSafeFloat(): Float? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.contentOrNull?.toFloatOrNull()
            else -> null
        }
    }

    /**
     * 解析 SauceNAO API 返回的 JSON 字符串
     */
    fun parse(jsonString: String): List<SearchResultItem> {
        if (jsonString.isBlank()) return emptyList()

        val rootElement = json.parseToJsonElement(jsonString)
        if (rootElement !is JsonObject) {
            return emptyList()
        }

        val rootHeader = rootElement["header"]?.let { if (it is JsonObject) it else null }
        val status = rootHeader?.get("status")?.asSafeInt() ?: 0
        val message = rootHeader?.get("message")?.asSafeString()

        if (status != 0) {
            throw SauceNaoException(status, message ?: "SauceNAO error status: $status")
        }

        val resultsArray = rootElement["results"]?.let { if (it is JsonArray) it else null } ?: return emptyList()
        val items = mutableListOf<SearchResultItem>()

        for (elem in resultsArray) {
            if (elem !is JsonObject) continue
            val header = elem["header"]?.let { if (it is JsonObject) it else null }
            val data = elem["data"]?.let { if (it is JsonObject) it else null }

            val similarity = header?.get("similarity")?.asSafeFloat()
            val thumbnail = header?.get("thumbnail")?.asSafeString()

            // 作品标题（安全解析字符串或数组）
            val title = data?.get("title")?.asSafeString()
                ?: data?.get("source")?.asSafeString()
                ?: data?.get("jp_name")?.asSafeString()
                ?: data?.get("eng_name")?.asSafeString()
                ?: data?.get("material")?.asSafeString()
                ?: header?.get("index_name")?.asSafeString()
                ?: ""

            // 作者/创作者
            val author = data?.get("member_name")?.asSafeString()
                ?: data?.get("author_name")?.asSafeString()
                ?: data?.get("twitter_user_handle")?.asSafeString()
                ?: data?.get("creator")?.asSafeString()

            // 外链列表
            val extUrls = mutableListOf<String>()
            data?.get("ext_urls")?.let { urlsElem ->
                if (urlsElem is JsonArray) {
                    for (u in urlsElem) {
                        u.asSafeString()?.let { extUrls.add(it) }
                    }
                } else {
                    urlsElem.asSafeString()?.let { extUrls.add(it) }
                }
            }
            val primarySourceUrl = extUrls.firstOrNull()

            // 附加额外信息
            val extraList = mutableListOf<Pair<Int, String>>()
            data?.get("pixiv_id")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_pixiv_id to it)
            }
            data?.get("tweet_id")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_tweet_id to it)
            }
            data?.get("part")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_episode to it)
            }
            data?.get("est_time")?.asSafeString()?.let {
                extraList.add(R.string.image_search_label_timeline to it)
            }

            items.add(
                SearchResultItem(
                    engine = SearchEngineEnum.SAUCENAO,
                    title = title,
                    similarity = similarity,
                    thumbnailUrl = thumbnail,
                    author = author,
                    sourceUrl = primarySourceUrl,
                    extraInfo = extraList
                )
            )
        }

        return items
    }
}
