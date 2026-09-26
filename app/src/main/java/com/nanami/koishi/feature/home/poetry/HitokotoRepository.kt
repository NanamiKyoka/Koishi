package com.nanami.koishi.feature.home.poetry

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex

/**
 * 每次取句优先复用本地池，池中不足时后台平滑补充，
 * 避免把每次打开应用与每次换一句都直接打到一言服务上
 */
class HitokotoRepository(private val cache: HitokotoCache) {

    private val fillLock = Mutex()

    private val shown = mutableSetOf<String>()

    suspend fun next(): Hitokoto? {
        cache.take(shown)?.let { cached ->
            shown += cached.uuid
            return cached
        }

        val fetched = runCatching { HitokotoClient.fetch() }.getOrNull()
        if (fetched == null) {
            return cache.take(shown)?.also { shown += it.uuid }
        }

        shown += fetched.uuid
        cache.store(fetched)
        return fetched
    }

    suspend fun refill() {
        if (!fillLock.tryLock()) return
        try {
            val missing = POOL_TARGET - cache.count()
            if (missing <= 0) return

            val batch = missing.coerceAtMost(REFILL_BATCH)
            coroutineScope {
                List(batch) { async { runCatching { HitokotoClient.fetch() }.getOrNull() } }
                    .awaitAll()
                    .filterNotNull()
                    .forEach { cache.store(it) }
            }
        } finally {
            fillLock.unlock()
        }
    }

    private companion object {
        const val POOL_TARGET = 9
        const val REFILL_BATCH = 3
    }
}
