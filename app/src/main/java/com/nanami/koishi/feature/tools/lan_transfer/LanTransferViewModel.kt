package com.nanami.koishi.feature.tools.lan_transfer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.StateFlow

class LanTransferViewModel(
    application: Application,
    private val engine: LanTransferEngine
) : AndroidViewModel(application) {

    val uiState: StateFlow<LanTransferUiState> = engine.uiState

    init {
        engine.initialize()
    }

    fun onEvent(event: LanTransferUiEvent) {
        engine.onEvent(event)
    }

    override fun onCleared() {
        engine.cancelPendingPinPrompt()
    }
}
