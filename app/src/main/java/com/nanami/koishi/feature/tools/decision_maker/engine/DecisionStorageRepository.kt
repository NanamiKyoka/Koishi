package com.nanami.koishi.feature.tools.decision_maker.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class DecisionMakerStore(
    val storedTopics: List<DecisionTopic> = emptyList(),
    val hiddenBuiltInIds: Set<String> = emptySet(),
    val selectedTopicId: String = BuiltInPresets.defaultTopicId,
    val mode: DecisionMode = DecisionMode.WHEEL,
    val hapticsEnabled: Boolean = true
)

class DecisionStorageRepository(
    dao: ToolStorageDao
) : BaseToolRepository<DecisionMakerStore>(
    toolId = TOOL_ID,
    serializer = DecisionMakerStore.serializer(),
    dao = dao,
    defaultData = DecisionMakerStore()
) {

    suspend fun selectTopic(topicId: String): DecisionMakerStore =
        updateData { it.copy(selectedTopicId = topicId) }

    suspend fun setMode(mode: DecisionMode): DecisionMakerStore =
        updateData { it.copy(mode = mode) }

    suspend fun setHapticsEnabled(enabled: Boolean): DecisionMakerStore =
        updateData { it.copy(hapticsEnabled = enabled) }

    companion object {
        const val TOOL_ID = "decision_maker"
    }
}
