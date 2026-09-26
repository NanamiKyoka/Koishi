package com.nanami.koishi.feature.tools.bmi_calculator

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCategory

enum class BmiTrendDirection {
    UP,
    DOWN,
    FLAT
}

data class BmiRecordUi(
    val id: String,
    val bmi: Double,
    val bmiText: String,
    val heightText: String,
    val weightText: String,
    val timeText: String,
    val axisLabel: String,
    val category: BmiCategory,
    val deltaDirection: BmiTrendDirection,
    val deltaText: String,
    val isLatest: Boolean
)

data class BmiCalculatorUiState(
    val heightInput: String = "",
    val weightInput: String = "",
    val heightError: Boolean = false,
    val weightError: Boolean = false,
    val previewBmiText: String = "",
    val previewCategory: BmiCategory? = null,
    val gaugeFraction: Float = 0f,
    val healthyWeightMinText: String = "",
    val healthyWeightMaxText: String = "",
    val healthyHintRes: Int? = null,
    val healthyHintArgs: List<String> = emptyList(),
    val trendRecords: List<BmiRecordUi> = emptyList(),
    val historyRecords: List<BmiRecordUi> = emptyList(),
    val selectedRecordId: String? = null,
    val selectedRecord: BmiRecordUi? = null,
    val showClearConfirm: Boolean = false,
    @StringRes val messageRes: Int? = null,
    val messageArgs: List<String> = emptyList(),
    val messageHasUndo: Boolean = false
) {
    val hasRecords: Boolean get() = trendRecords.isNotEmpty()
}

sealed interface BmiCalculatorUiEvent {
    data class OnHeightChange(val value: String) : BmiCalculatorUiEvent
    data class OnWeightChange(val value: String) : BmiCalculatorUiEvent
    data object OnSaveRecord : BmiCalculatorUiEvent
    data class OnSelectRecord(val recordId: String) : BmiCalculatorUiEvent
    data object OnClearSelection : BmiCalculatorUiEvent
    data class OnDeleteRecord(val recordId: String) : BmiCalculatorUiEvent
    data object OnUndoDelete : BmiCalculatorUiEvent
    data object OnRequestClearAll : BmiCalculatorUiEvent
    data object OnConfirmClearAll : BmiCalculatorUiEvent
    data object OnDismissClearAll : BmiCalculatorUiEvent
    data object OnDismissMessage : BmiCalculatorUiEvent
}
