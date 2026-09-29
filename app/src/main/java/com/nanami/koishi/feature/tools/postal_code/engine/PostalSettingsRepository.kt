package com.nanami.koishi.feature.tools.postal_code.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class PostalSettings(
    val codeToRegionHistory: List<String> = emptyList(),
    val regionToCodeHistory: List<String> = emptyList()
) {
    fun historyOf(direction: PostalDirection): List<String> = when (direction) {
        PostalDirection.CODE_TO_REGION -> codeToRegionHistory
        PostalDirection.REGION_TO_CODE -> regionToCodeHistory
    }
}

class PostalSettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<PostalSettings>(
    toolId = TOOL_ID,
    serializer = PostalSettings.serializer(),
    dao = dao,
    defaultData = PostalSettings()
) {

    suspend fun recordQuery(direction: PostalDirection, keyword: String): PostalSettings =
        updateData { current ->
            val trimmed = keyword.trim()
            if (trimmed.isEmpty()) return@updateData current

            val existing = current.historyOf(direction)
            val updated = (listOf(trimmed) + existing.filter { it != trimmed }).take(MAX_HISTORY)
            when (direction) {
                PostalDirection.CODE_TO_REGION -> current.copy(codeToRegionHistory = updated)
                PostalDirection.REGION_TO_CODE -> current.copy(regionToCodeHistory = updated)
            }
        }

    suspend fun clearHistory(direction: PostalDirection): PostalSettings = updateData { current ->
        when (direction) {
            PostalDirection.CODE_TO_REGION -> current.copy(codeToRegionHistory = emptyList())
            PostalDirection.REGION_TO_CODE -> current.copy(regionToCodeHistory = emptyList())
        }
    }

    companion object {
        const val TOOL_ID = "postal_code"
        private const val MAX_HISTORY = 12
    }
}
