package com.nanami.koishi.core.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import java.util.concurrent.ConcurrentHashMap

/**
 * 工具数据仓库抽象层，负责 JSON 序列化、默认值回退与读改写串行化，
 * 子类只需声明数据类型与对外业务方法，无需再接触 Room 或 Json 细节
 */
abstract class BaseToolRepository<T>(
    val toolId: String,
    private val serializer: KSerializer<T>,
    private val dao: ToolStorageDao,
    protected val defaultData: T
) {

    private val json = ToolStorageJson.instance

    private val writeLock: Mutex = locks.computeIfAbsent(toolId) { Mutex() }

    /**
     * 存储中不存在记录、载荷解析失败时均会回退到 [defaultData]，订阅方无需处理 null
     */
    val dataFlow: Flow<T> = dao.observe(toolId)
        .map { entity -> entity?.let { decode(it.payloadJson) } ?: defaultData }
        .catch { emit(defaultData) }
        .distinctUntilChanged()

    suspend fun currentData(): T =
        dao.find(toolId)?.let { decode(it.payloadJson) } ?: defaultData

    /**
     * 在写锁保护下完成读取、变换与落库，同一 toolId 的并发调用不会互相覆盖
     */
    suspend fun updateData(transform: (T) -> T): T = writeLock.withLock {
        val current = currentData()
        val updated = transform(current)
        if (updated != current) persist(updated)
        updated
    }

    suspend fun setData(data: T): T = writeLock.withLock {
        persist(data)
        data
    }

    suspend fun hasStoredData(): Boolean = dao.find(toolId) != null

    suspend fun reset(): Boolean = writeLock.withLock {
        dao.delete(toolId) > 0
    }

    private suspend fun persist(data: T) {
        dao.upsert(
            ToolStorageEntity(
                toolId = toolId,
                payloadJson = json.encodeToString(serializer, data),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun decode(payloadJson: String): T = try {
        json.decodeFromString(serializer, payloadJson)
    } catch (error: Exception) {
        onDecodeFailed(error)
        defaultData
    }

    protected open fun onDecodeFailed(error: Exception) = Unit

    private companion object {
        private val locks = ConcurrentHashMap<String, Mutex>()
    }
}
