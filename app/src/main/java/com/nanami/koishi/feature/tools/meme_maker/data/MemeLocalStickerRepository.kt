package com.nanami.koishi.feature.tools.meme_maker.data

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable

@Serializable
data class MemeLocalSticker(
    val uri: String,
    val name: String,
    val addedAt: Long = 0L
)

@Serializable
data class MemeLocalStickerLibrary(
    val stickers: List<MemeLocalSticker> = emptyList()
)

/**
 * 用户自行导入的贴纸素材，只保留相册引用链接而不复制文件本体，
 * 原图在系统中被删除后对应素材同步失效
 */
class MemeLocalStickerRepository(dao: ToolStorageDao) : BaseToolRepository<MemeLocalStickerLibrary>(
    toolId = TOOL_ID,
    serializer = MemeLocalStickerLibrary.serializer(),
    dao = dao,
    defaultData = MemeLocalStickerLibrary()
) {

    suspend fun add(uri: String, name: String): List<MemeLocalSticker> =
        updateData { current ->
            if (current.stickers.any { it.uri == uri }) {
                current
            } else {
                current.copy(
                    stickers = current.stickers + MemeLocalSticker(
                        uri = uri,
                        name = name,
                        addedAt = System.currentTimeMillis()
                    )
                )
            }
        }.stickers

    suspend fun remove(uri: String): List<MemeLocalSticker> =
        updateData { current ->
            current.copy(stickers = current.stickers.filterNot { it.uri == uri })
        }.stickers

    suspend fun retainOnly(readableUris: Set<String>): List<MemeLocalSticker> =
        updateData { current ->
            current.copy(stickers = current.stickers.filter { it.uri in readableUris })
        }.stickers

    companion object {
        const val TOOL_ID = "meme_local_stickers"
    }
}
