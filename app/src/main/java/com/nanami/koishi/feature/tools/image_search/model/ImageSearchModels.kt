package com.nanami.koishi.feature.tools.image_search.model

import androidx.annotation.StringRes
import com.nanami.koishi.R

/**
 * 支持的以图搜图反向检索引擎
 */
enum class SearchEngineEnum(
    val id: String,
    @StringRes val displayNameRes: Int,
    @StringRes val descRes: Int,
    val homeUrl: String
) {
    SAUCENAO(
        id = "saucenao",
        displayNameRes = R.string.image_search_engine_saucenao,
        descRes = R.string.image_search_engine_saucenao_desc,
        homeUrl = "https://saucenao.com/"
    ),
    TRACE_MOE(
        id = "trace_moe",
        displayNameRes = R.string.image_search_engine_trace_moe,
        descRes = R.string.image_search_engine_trace_moe_desc,
        homeUrl = "https://trace.moe/"
    ),
    GOOGLE_LENS(
        id = "google_lens",
        displayNameRes = R.string.image_search_engine_google_lens,
        descRes = R.string.image_search_engine_google_lens_desc,
        homeUrl = "https://lens.google.com/"
    )
}

/**
 * 搜索引擎当前的检索状态
 */
enum class EngineSearchStatus {
    IDLE,
    SEARCHING,
    SUCCESS,
    EMPTY,
    ERROR,
    FALLBACK_REQUIRED,
    CONFIG_REQUIRED
}

/**
 * 归一化的以图搜图结果项
 */
data class SearchResultItem(
    val engine: SearchEngineEnum,
    val title: String,
    val similarity: Float? = null,
    val thumbnailUrl: String? = null,
    val author: String? = null,
    val sourceUrl: String? = null,
    val extraInfo: List<Pair<Int, String>> = emptyList()
)

/**
 * 单个搜索引擎的实时搜索状态
 */
data class EngineSearchState(
    val engine: SearchEngineEnum,
    val status: EngineSearchStatus = EngineSearchStatus.IDLE,
    val results: List<SearchResultItem> = emptyList(),
    @StringRes val errorMessageRes: Int? = null,
    val errorMessage: String? = null,
    val fallbackUrl: String? = null
)
