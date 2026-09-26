package com.nanami.koishi.feature.tools.mini_apps.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlin.math.abs

/**
 * 轻应用条目仓库，条目按添加时间倒序排列，重复网址与自定义名称由仓库统一把关
 */
class MiniAppStore(dao: ToolStorageDao) : BaseToolRepository<MiniAppLibrary>(
    toolId = TOOL_ID,
    serializer = MiniAppLibrary.serializer(),
    dao = dao,
    defaultData = MiniAppLibrary()
) {

    suspend fun add(
        rawUrl: String,
        rawTitle: String,
        now: Long = System.currentTimeMillis()
    ): MiniAppAddResult {
        val url = MiniAppUrl.normalize(rawUrl) ?: return MiniAppAddResult.InvalidUrl
        val customTitle = rawTitle.trim()
        var result: MiniAppAddResult = MiniAppAddResult.Duplicate

        updateData { library ->
            if (library.entries.any { it.url.equals(url, ignoreCase = true) }) {
                library
            } else {
                val entry = MiniAppEntry(
                    id = "mini_app_${now}_${abs(url.hashCode())}",
                    url = url,
                    title = customTitle.ifEmpty { MiniAppUrl.hostOf(url) ?: url },
                    hasCustomTitle = customTitle.isNotEmpty(),
                    addedAt = now
                )
                result = MiniAppAddResult.Added(entry)
                library.copy(entries = listOf(entry) + library.entries)
            }
        }
        return result
    }

    suspend fun remove(entryId: String) {
        updateData { library ->
            library.copy(entries = library.entries.filterNot { it.id == entryId })
        }
    }

    /**
     * 长按拖动后的新顺序落库，越界或原地移动直接忽略
     */
    suspend fun move(fromIndex: Int, toIndex: Int) {
        updateData { library ->
            val entries = library.entries
            if (fromIndex == toIndex || fromIndex !in entries.indices || toIndex !in entries.indices) {
                library
            } else {
                val reordered = entries.toMutableList()
                reordered.add(toIndex, reordered.removeAt(fromIndex))
                library.copy(entries = reordered)
            }
        }
    }

    suspend fun setDesktopMode(entryId: String, enabled: Boolean) {
        updateData { library ->
            library.copy(
                entries = library.entries.map { entry ->
                    if (entry.id == entryId && entry.desktopMode != enabled) {
                        entry.copy(desktopMode = enabled)
                    } else {
                        entry
                    }
                }
            )
        }
    }

    /**
     * 用网页真实标题回填条目名称，自定义名称与无意义标题都会被忽略
     */
    suspend fun applyPageTitle(entryId: String, title: String, url: String) {
        if (!MiniAppUrl.isMeaningfulTitle(title, url)) return
        val pageTitle = title.trim()

        updateData { library ->
            library.copy(
                entries = library.entries.map { entry ->
                    if (entry.id != entryId || entry.hasCustomTitle || entry.title == pageTitle) {
                        entry
                    } else {
                        entry.copy(title = pageTitle)
                    }
                }
            )
        }
    }

    companion object {
        const val TOOL_ID = "mini_apps"
    }
}
