package com.nanami.koishi.feature.tools.decision_maker.engine

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

sealed interface TopicImportResult {
    data class Success(val added: Int, val replaced: Int) : TopicImportResult
    data object Empty : TopicImportResult
    data object Malformed : TopicImportResult
}

/**
 * 主题仓库：内置预设始终由资源实时生成，用户改动以覆盖副本形式落盘，删除内置项即还原默认
 */
class DecisionRepository(
    private val context: Context,
    private val preferences: DecisionPreferences
) {

    private var stored: List<DecisionTopic> = preferences.storedTopics
    private var hiddenBuiltIns: Set<String> = preferences.hiddenBuiltInIds

    private val _topics = MutableStateFlow(loadEffective())
    val topics: StateFlow<List<DecisionTopic>> = _topics.asStateFlow()

    fun findTopic(topicId: String): DecisionTopic? = _topics.value.firstOrNull { it.id == topicId }

    fun saveTopic(topic: DecisionTopic): DecisionTopic {
        val sanitized = topic.sanitized()
        val mutable = stored.toMutableList()
        val index = mutable.indexOfFirst { it.id == sanitized.id }
        if (index >= 0) mutable[index] = sanitized else mutable.add(sanitized)
        apply(stored = mutable, hidden = hiddenBuiltIns - sanitized.id)
        return sanitized
    }

    fun deleteTopic(topicId: String) {
        apply(
            stored = stored.filterNot { it.id == topicId },
            hidden = if (topicId in BuiltInPresets.ids) hiddenBuiltIns + topicId else hiddenBuiltIns
        )
    }

    fun restoreBuiltIns() {
        apply(
            stored = stored.filterNot { it.id in BuiltInPresets.ids },
            hidden = emptySet()
        )
    }

    fun exportJson(): String = DecisionArchiveCodec.encode(_topics.value)

    fun importJson(raw: String): TopicImportResult {
        val decoded = DecisionArchiveCodec.decode(raw) ?: return TopicImportResult.Malformed

        val incoming = decoded
            .map { it.sanitized() }
            .filter { it.title.isNotBlank() && it.options.isNotEmpty() }
        if (incoming.isEmpty()) return TopicImportResult.Empty

        val mutable = stored.toMutableList()
        var added = 0
        var replaced = 0
        incoming.forEach { topic ->
            val index = mutable.indexOfFirst { it.id == topic.id }
            if (index >= 0) {
                mutable[index] = topic
                replaced++
            } else {
                mutable.add(topic)
                added++
            }
        }
        apply(stored = mutable, hidden = hiddenBuiltIns - incoming.map { it.id }.toSet())
        return TopicImportResult.Success(added, replaced)
    }

    suspend fun writeTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(exportJson().toByteArray())
            } ?: return@withContext false
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun readFrom(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().toString(Charsets.UTF_8)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun apply(stored: List<DecisionTopic>, hidden: Set<String>) {
        this.stored = stored
        this.hiddenBuiltIns = hidden
        preferences.storedTopics = stored
        preferences.hiddenBuiltInIds = hidden
        _topics.value = loadEffective()
    }

    private fun loadEffective(): List<DecisionTopic> {
        val overrides = stored.associateBy { it.id }
        val builtIns = BuiltInPresets.build(context)
            .filterNot { it.id in hiddenBuiltIns }
            .map { preset -> overrides[preset.id]?.copy(id = preset.id) ?: preset }
        val custom = stored.filterNot { it.id in BuiltInPresets.ids }
        return builtIns + custom
    }
}
