package com.nanami.koishi.feature.home.poetry

import com.nanami.koishi.core.data.storage.ToolStorageDao
import com.nanami.koishi.core.data.storage.ToolStorageEntity
import com.nanami.koishi.core.data.storage.ToolStorageJson
import kotlinx.serialization.Serializable

@Serializable
private data class CachedHitokoto(
    val uuid: String,
    val text: String,
    val source: String = "",
    val author: String = ""
)

@Serializable
private data class HitokotoPool(
    val sentences: List<CachedHitokoto> = emptyList()
)

class HitokotoCache(private val dao: ToolStorageDao) {

    private val json = ToolStorageJson.instance

    suspend fun take(excluded: Set<String>): Hitokoto? {
        val pool = read()
        val candidate = pool.sentences
            .filterNot { it.uuid in excluded }
            .randomOrNull() ?: return null

        write(pool.sentences - candidate)
        return candidate.toHitokoto()
    }

    suspend fun store(sentence: Hitokoto) {
        val rest = read().sentences.filterNot { it.uuid == sentence.uuid }
        val merged = rest + CachedHitokoto(sentence.uuid, sentence.text, sentence.source, sentence.author)
        write(if (merged.size > POOL_LIMIT) merged.takeLast(POOL_LIMIT) else merged)
    }

    suspend fun count(): Int = read().sentences.size

    private suspend fun read(): HitokotoPool {
        val payload = dao.find(STORAGE_KEY)?.payloadJson ?: return HitokotoPool()
        return try {
            json.decodeFromString(HitokotoPool.serializer(), payload)
        } catch (e: Exception) {
            HitokotoPool()
        }
    }

    private suspend fun write(sentences: List<CachedHitokoto>) {
        dao.upsert(
            ToolStorageEntity(
                toolId = STORAGE_KEY,
                payloadJson = json.encodeToString(
                    HitokotoPool.serializer(),
                    HitokotoPool(sentences)
                ),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun CachedHitokoto.toHitokoto() = Hitokoto(uuid, text, source, author)

    private companion object {
        const val STORAGE_KEY = "hitokoto.pool"
        const val POOL_LIMIT = 40
    }
}
