package com.nanami.koishi.core.data.storage

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * 内存版 DAO，用于在 JVM 单元测试中替代 Room 生成的实现
 */
class FakeToolStorageDao : ToolStorageDao {

    var readDelayMillis: Long = 0

    var upsertCount: Int = 0
        private set

    private val rows = MutableStateFlow<Map<String, ToolStorageEntity>>(emptyMap())

    override fun observe(toolId: String): Flow<ToolStorageEntity?> = rows.map { it[toolId] }

    override fun observeAll(): Flow<List<ToolStorageEntity>> = rows.map { it.values.toList() }

    override suspend fun find(toolId: String): ToolStorageEntity? {
        if (readDelayMillis > 0) delay(readDelayMillis)
        return rows.value[toolId]
    }

    override suspend fun upsert(entity: ToolStorageEntity) {
        upsertCount++
        rows.update { it + (entity.toolId to entity) }
    }

    override suspend fun upsertAll(entities: List<ToolStorageEntity>) {
        rows.update { current -> current + entities.associateBy { it.toolId } }
    }

    override suspend fun delete(toolId: String): Int {
        val existed = rows.value.containsKey(toolId)
        rows.update { it - toolId }
        return if (existed) 1 else 0
    }

    override suspend fun deleteAll(): Int {
        val count = rows.value.size
        rows.value = emptyMap()
        return count
    }
}
