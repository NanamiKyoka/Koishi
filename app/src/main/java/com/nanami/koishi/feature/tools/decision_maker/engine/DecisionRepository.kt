package com.nanami.koishi.feature.tools.decision_maker.engine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

sealed interface TopicImportResult {
    data class Success(val added: Int, val replaced: Int) : TopicImportResult
    data object Empty : TopicImportResult
    data object Malformed : TopicImportResult
}

/**
 * 主题仓库：内置预设始终由资源实时生成，用户改动以覆盖副本形式落盘，删除内置项即还原默认
 */
class DecisionRepository(
    private val builtInTopics: () -> List<DecisionTopic>,
    private val storage: DecisionStorageRepository
) {

    val state: Flow<DecisionMakerStore> = storage.dataFlow

    val topics: Flow<List<DecisionTopic>> = storage.dataFlow
        .map { effectiveTopics(it) }
        .distinctUntilChanged()

    suspend fun findTopic(topicId: String): DecisionTopic? =
        topics.first().firstOrNull { it.id == topicId }

    suspend fun saveTopic(topic: DecisionTopic): DecisionTopic {
        val sanitized = topic.sanitized()
        storage.updateData { store ->
            val mutable = store.storedTopics.toMutableList()
            val index = mutable.indexOfFirst { it.id == sanitized.id }
            if (index >= 0) mutable[index] = sanitized else mutable.add(sanitized)
            store.copy(
                storedTopics = mutable,
                hiddenBuiltInIds = store.hiddenBuiltInIds - sanitized.id
            )
        }
        return sanitized
    }

    suspend fun deleteTopic(topicId: String) {
        val builtInIds = builtInTopicIds()
        storage.updateData { store ->
            store.copy(
                storedTopics = store.storedTopics.filterNot { it.id == topicId },
                hiddenBuiltInIds = if (topicId in builtInIds) {
                    store.hiddenBuiltInIds + topicId
                } else {
                    store.hiddenBuiltInIds
                }
            )
        }
    }

    suspend fun restoreBuiltIns() {
        val builtInIds = builtInTopicIds()
        storage.updateData { store ->
            store.copy(
                storedTopics = store.storedTopics.filterNot { it.id in builtInIds },
                hiddenBuiltInIds = emptySet()
            )
        }
    }

    suspend fun selectTopic(topicId: String) = storage.selectTopic(topicId)

    suspend fun setMode(mode: DecisionMode) = storage.setMode(mode)

    suspend fun setHapticsEnabled(enabled: Boolean) = storage.setHapticsEnabled(enabled)

    suspend fun exportJson(): String = DecisionArchiveCodec.encode(topics.first())

    suspend fun importJson(raw: String): TopicImportResult {
        val decoded = DecisionArchiveCodec.decode(raw) ?: return TopicImportResult.Malformed

        val incoming = decoded
            .map { it.sanitized() }
            .filter { it.title.isNotBlank() && it.options.isNotEmpty() }
        if (incoming.isEmpty()) return TopicImportResult.Empty

        var added = 0
        var replaced = 0
        storage.updateData { store ->
            val mutable = store.storedTopics.toMutableList()
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
            store.copy(
                storedTopics = mutable,
                hiddenBuiltInIds = store.hiddenBuiltInIds - incoming.map { it.id }.toSet()
            )
        }
        return TopicImportResult.Success(added, replaced)
    }

    private fun builtInTopicIds(): Set<String> = builtInTopics().map { it.id }.toSet()

    fun effectiveTopics(store: DecisionMakerStore): List<DecisionTopic> {
        val presets = builtInTopics()
        val presetIds = presets.map { it.id }
        val overrides = store.storedTopics.associateBy { it.id }
        val builtIns = presets
            .filterNot { it.id in store.hiddenBuiltInIds }
            .map { preset -> overrides[preset.id]?.copy(id = preset.id) ?: preset }
        val custom = store.storedTopics.filterNot { it.id in presetIds }
        return builtIns + custom
    }
}
