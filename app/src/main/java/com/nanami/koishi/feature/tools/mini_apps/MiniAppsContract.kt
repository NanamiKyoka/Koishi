package com.nanami.koishi.feature.tools.mini_apps

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppEntry

data class MiniAppsUiState(
    val entries: List<MiniAppEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isAddDialogVisible: Boolean = false,
    @StringRes val userMessageRes: Int? = null,
    val userMessageArgs: List<String> = emptyList()
) {
    val isEmpty: Boolean
        get() = !isLoading && entries.isEmpty()

    val hasEntries: Boolean
        get() = entries.isNotEmpty()
}

sealed interface MiniAppsUiEvent {
    data class OnShowAddDialog(val show: Boolean) : MiniAppsUiEvent
    data class OnAddEntry(val url: String, val title: String) : MiniAppsUiEvent
    data class OnRemoveEntry(val entryId: String) : MiniAppsUiEvent
    data class OnMoveEntry(val fromIndex: Int, val toIndex: Int) : MiniAppsUiEvent
    data object OnDismissMessage : MiniAppsUiEvent
}
