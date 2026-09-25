package com.nanami.koishi.feature.tools.decision_maker.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class DecisionArchive(
    val version: Int = 1,
    val topics: List<DecisionTopic> = emptyList()
)

object DecisionArchiveCodec {

    const val CURRENT_VERSION = 1

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(topics: List<DecisionTopic>): String =
        json.encodeToString(DecisionArchive(version = CURRENT_VERSION, topics = topics))

    fun decode(raw: String): List<DecisionTopic>? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        return try {
            json.decodeFromString<DecisionArchive>(trimmed).topics
        } catch (e: Exception) {
            null
        }
    }
}
