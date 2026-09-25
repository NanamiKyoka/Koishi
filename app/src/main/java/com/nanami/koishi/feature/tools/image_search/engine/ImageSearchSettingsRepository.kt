package com.nanami.koishi.feature.tools.image_search.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchSettings(
    val sauceNaoApiKey: String = ""
)

class ImageSearchSettingsRepository(
    dao: ToolStorageDao
) : BaseToolRepository<ImageSearchSettings>(
    toolId = TOOL_ID,
    serializer = ImageSearchSettings.serializer(),
    dao = dao,
    defaultData = ImageSearchSettings()
) {

    suspend fun saveApiKey(apiKey: String): ImageSearchSettings =
        updateData { it.copy(sauceNaoApiKey = apiKey.trim()) }

    companion object {
        const val TOOL_ID = "image_search"
    }
}
