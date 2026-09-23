package com.nanami.koishi.feature.tools.image_search

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.image_search.model.EngineSearchState
import com.nanami.koishi.feature.tools.image_search.model.SearchEngineEnum

/**
 * 以图搜图 UI 状态
 */
data class ImageSearchUiState(
    val selectedImageUri: Uri? = null,
    val previewBitmap: Bitmap? = null,
    val selectedEngines: Set<SearchEngineEnum> = setOf(
        SearchEngineEnum.SAUCENAO,
        SearchEngineEnum.TRACE_MOE,
        SearchEngineEnum.ASCII2D,
        SearchEngineEnum.GOOGLE_LENS
    ),
    val collapsedEngines: Set<SearchEngineEnum> = emptySet(),
    val sauceNaoApiKey: String = "",
    val showApiKeyDialog: Boolean = false,
    val isSearching: Boolean = false,
    val engineStates: Map<SearchEngineEnum, EngineSearchState> = emptyMap(),
    @StringRes val userMessageRes: Int? = null,
    val userMessageArgs: List<String> = emptyList()
) {
    val canSearch: Boolean
        get() = selectedImageUri != null && selectedEngines.isNotEmpty() && !isSearching

    val hasResults: Boolean
        get() = engineStates.values.any { it.results.isNotEmpty() }
}

/**
 * 以图搜图 UI 事件
 */
sealed interface ImageSearchUiEvent {
    data class OnImageSelected(val uri: Uri) : ImageSearchUiEvent
    data object OnClearImage : ImageSearchUiEvent
    data class OnToggleEngine(val engine: SearchEngineEnum) : ImageSearchUiEvent
    data class OnToggleEngineCollapse(val engine: SearchEngineEnum) : ImageSearchUiEvent
    data class OnShowApiKeyDialog(val show: Boolean) : ImageSearchUiEvent
    data class OnSaveSauceNaoApiKey(val key: String) : ImageSearchUiEvent
    data object OnStartSearch : ImageSearchUiEvent
    data object OnDismissMessage : ImageSearchUiEvent
}
