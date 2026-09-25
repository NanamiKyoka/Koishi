package com.nanami.koishi.core.data.storage.sample

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class ImageSearchConfig(
    val sauceNaoApiKey: String = "",
    val autoSearchOnPick: Boolean = false,
    val uploadMaxEdge: Int = DEFAULT_UPLOAD_MAX_EDGE,
    val hapticsEnabled: Boolean = true
) {
    companion object {
        const val DEFAULT_UPLOAD_MAX_EDGE = 1280
        const val MIN_UPLOAD_MAX_EDGE = 320
        const val MAX_UPLOAD_MAX_EDGE = 4096
    }
}

/**
 * 配置型工具示例：单个 JSON 对象承载全部设置项，适合 API Key、开关、数值阈值这类扁平配置
 */
class ImageSearchConfigRepository(
    dao: ToolStorageDao
) : BaseToolRepository<ImageSearchConfig>(
    toolId = TOOL_ID,
    serializer = ImageSearchConfig.serializer(),
    dao = dao,
    defaultData = ImageSearchConfig()
) {

    suspend fun setApiKey(apiKey: String): ImageSearchConfig =
        updateData { it.copy(sauceNaoApiKey = apiKey.trim()) }

    suspend fun setAutoSearchOnPick(enabled: Boolean): ImageSearchConfig =
        updateData { it.copy(autoSearchOnPick = enabled) }

    suspend fun setUploadMaxEdge(edge: Int): ImageSearchConfig =
        updateData {
            it.copy(
                uploadMaxEdge = edge.coerceIn(
                    ImageSearchConfig.MIN_UPLOAD_MAX_EDGE,
                    ImageSearchConfig.MAX_UPLOAD_MAX_EDGE
                )
            )
        }

    suspend fun toggleHaptics(): ImageSearchConfig =
        updateData { it.copy(hapticsEnabled = !it.hapticsEnabled) }

    companion object {
        const val TOOL_ID = "image_search_config"
    }
}
