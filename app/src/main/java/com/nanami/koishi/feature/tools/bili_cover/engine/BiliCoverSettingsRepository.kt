package com.nanami.koishi.feature.tools.bili_cover.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class BiliCoverSettings(
    val recentQueries: List<String> = emptyList()
)

class BiliCoverSettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<BiliCoverSettings>(
    toolId = TOOL_ID,
    serializer = BiliCoverSettings.serializer(),
    dao = dao,
    defaultData = BiliCoverSettings()
) {

    suspend fun recordQuery(input: String): BiliCoverSettings = updateData { settings ->
        val normalized = input.trim()
        if (normalized.isEmpty()) {
            settings
        } else {
            settings.copy(
                recentQueries = (
                        listOf(normalized) +
                                settings.recentQueries.filterNot { it.equals(normalized, ignoreCase = true) }
                        ).take(MAX_RECENT)
            )
        }
    }

    suspend fun clearRecent(): BiliCoverSettings =
        updateData { it.copy(recentQueries = emptyList()) }

    companion object {
        const val TOOL_ID = "bili_cover"
        const val MAX_RECENT = 8
    }
}
