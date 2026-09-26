package com.nanami.koishi.feature.tools.mini_apps

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.KoishiApp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppAddResult
import com.nanami.koishi.feature.tools.mini_apps.engine.MiniAppStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MiniAppsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = MiniAppStore((application as KoishiApp).toolStorageDao)

    private val _uiState = MutableStateFlow(MiniAppsUiState())
    val uiState: StateFlow<MiniAppsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            store.dataFlow.collect { library ->
                _uiState.update { it.copy(entries = library.entries, isLoading = false) }
            }
        }
    }

    fun onEvent(event: MiniAppsUiEvent) {
        when (event) {
            is MiniAppsUiEvent.OnShowAddDialog ->
                _uiState.update { it.copy(isAddDialogVisible = event.show) }
            is MiniAppsUiEvent.OnAddEntry -> addEntry(event.url, event.title)
            is MiniAppsUiEvent.OnRemoveEntry -> removeEntry(event.entryId)
            is MiniAppsUiEvent.OnMoveEntry -> moveEntry(event.fromIndex, event.toIndex)
            is MiniAppsUiEvent.OnDismissMessage ->
                _uiState.update { it.copy(userMessageRes = null, userMessageArgs = emptyList()) }
        }
    }

    /**
     * 网页加载完成后回填真实标题，用户自定义名称不受影响
     */
    fun resolvePageTitle(entryId: String, title: String, url: String) {
        viewModelScope.launch { store.applyPageTitle(entryId, title, url) }
    }

    fun setDesktopMode(entryId: String, enabled: Boolean) {
        viewModelScope.launch { store.setDesktopMode(entryId, enabled) }
    }

    private fun moveEntry(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch { store.move(fromIndex, toIndex) }
    }

    private fun addEntry(url: String, title: String) {
        viewModelScope.launch {
            when (val result = store.add(url, title)) {
                is MiniAppAddResult.Added -> {
                    _uiState.update { it.copy(isAddDialogVisible = false) }
                    message(R.string.mini_apps_added, result.entry.title)
                }
                MiniAppAddResult.InvalidUrl -> message(R.string.mini_apps_error_invalid_url)
                MiniAppAddResult.Duplicate -> message(R.string.mini_apps_error_duplicate)
            }
        }
    }

    private fun removeEntry(entryId: String) {
        val entry = _uiState.value.entries.firstOrNull { it.id == entryId }
        viewModelScope.launch {
            store.remove(entryId)
            if (entry != null) message(R.string.mini_apps_removed, entry.title)
        }
    }

    private fun message(@StringRes res: Int, vararg args: String) {
        _uiState.update { it.copy(userMessageRes = res, userMessageArgs = args.toList()) }
    }
}
