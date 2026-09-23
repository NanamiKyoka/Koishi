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
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.Locale

object TraceMoeParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private fun JsonElement?.asSafeString(): String? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.contentOrNull
            is JsonArray -> this.mapNotNull { it.asSafeString() }.joinToString(", ").ifBlank { null }
            else -> null
        }
    }

    private fun JsonElement?.asSafeDouble(): Double? {
        if (this == null) return null
        return when (this) {
            is JsonPrimitive -> this.doubleOrNull
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

    /**
     * 解析 trace.moe API 返回的 JSON 字符串
     */
    fun parse(jsonString: String): List<SearchResultItem> {
        if (jsonString.isBlank()) return emptyList()

        val rootElement = json.parseToJsonElement(jsonString)
        if (rootElement !is JsonObject) return emptyList()

        val errorMsg = rootElement["error"]?.asSafeString()
        if (!errorMsg.isNullOrBlank()) {
            throw IllegalStateException(errorMsg)
        }

        val resultArray = rootElement["result"]?.let { if (it is JsonArray) it else null } ?: return emptyList()
        val items = mutableListOf<SearchResultItem>()

        for (elem in resultArray) {
            if (elem !is JsonObject) continue

            val anilistObj = elem["anilist"]?.let { if (it is JsonObject) it else null }
            val titleObj = anilistObj?.get("title")?.let { if (it is JsonObject) it else null }
            val anilistId = anilistObj?.get("id")?.asSafeInt()

            val nativeTitle = titleObj?.get("native")?.asSafeString()
            val romajiTitle = titleObj?.get("romaji")?.asSafeString()
            val englishTitle = titleObj?.get("english")?.asSafeString()
            val filename = elem["filename"]?.asSafeString()

            val title = nativeTitle
                ?: romajiTitle
                ?: englishTitle
                ?: filename
                ?: ""

            // 相似度: trace.moe 范围为 0.0 - 1.0，转为百分比 0..100
            val rawSimilarity = elem["similarity"]?.asSafeDouble()
            val similarity = rawSimilarity?.let { (it * 100).toFloat() }

            val episodeValue = elem["episode"]?.asSafeString() ?: "1"
            val fromSec = elem["from"]?.asSafeDouble() ?: 0.0
            val toSec = elem["to"]?.asSafeDouble() ?: 0.0

            val timeFormatted = "${formatSeconds(fromSec)} - ${formatSeconds(toSec)}"

            val imageUrl = elem["image"]?.asSafeString()
            val sourceUrl = anilistId?.let { "https://anilist.co/anime/$it" }

            val extraList = mutableListOf<Pair<Int, String>>()
            extraList.add(R.string.image_search_label_episode to episodeValue)
            extraList.add(R.string.image_search_label_timeline to timeFormatted)
            if (!romajiTitle.isNullOrBlank() && romajiTitle != title) {
                extraList.add(R.string.image_search_label_romaji to romajiTitle)
            }
            if (!englishTitle.isNullOrBlank() && englishTitle != title) {
                extraList.add(R.string.image_search_label_english to englishTitle)
            }

            items.add(
                SearchResultItem(
                    engine = SearchEngineEnum.TRACE_MOE,
                    title = title,
                    similarity = similarity,
                    thumbnailUrl = imageUrl,
                    author = null,
                    sourceUrl = sourceUrl,
                    extraInfo = extraList
                )
            )
        }

        return items
    }

    fun formatSeconds(totalSeconds: Double): String {
        val totalSec = totalSeconds.toInt()
        val hours = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }
}
