package com.nanami.koishi.feature.tools.mini_apps.engine

import kotlinx.serialization.Serializable

/**
 * 轻应用条目，[hasCustomTitle] 为 false 时允许在浏览过程中用网页真实标题覆盖 [title]
 */
@Serializable
data class MiniAppEntry(
    val id: String,
    val url: String,
    val title: String,
    val hasCustomTitle: Boolean = false,
    val addedAt: Long = 0L,
    val desktopMode: Boolean = false
)

@Serializable
data class MiniAppLibrary(
    val entries: List<MiniAppEntry> = emptyList()
)

sealed interface MiniAppAddResult {
    data class Added(val entry: MiniAppEntry) : MiniAppAddResult
    data object InvalidUrl : MiniAppAddResult
    data object Duplicate : MiniAppAddResult
}
