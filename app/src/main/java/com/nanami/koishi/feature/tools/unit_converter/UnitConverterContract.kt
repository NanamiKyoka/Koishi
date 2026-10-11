package com.nanami.koishi.feature.tools.unit_converter

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitCategory
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitDefinition

enum class UnitPickerTarget {
    FROM,
    TO
}

data class UnitConverterUiState(
    val category: UnitCategory,
    val fromUnit: UnitDefinition,
    val toUnit: UnitDefinition,
    val input: String = "1",
    val resultText: String = "",
    val reverseText: String = "",
    val pickerTarget: UnitPickerTarget? = null,
    @StringRes val messageRes: Int? = null,
    @StringRes val inputErrorRes: Int? = null,
    val copiedText: String = ""
)

sealed interface UnitConverterUiEvent {
    data class OnCategorySelected(val category: UnitCategory) : UnitConverterUiEvent
    data class OnInputChange(val value: String) : UnitConverterUiEvent
    data class OnUnitSelected(val target: UnitPickerTarget, val unitId: String) : UnitConverterUiEvent
    data object OnSwapUnits : UnitConverterUiEvent
    data class OnOpenPicker(val target: UnitPickerTarget) : UnitConverterUiEvent
    data object OnDismissPicker : UnitConverterUiEvent
    data object OnCopy : UnitConverterUiEvent
    data object OnMessageShown : UnitConverterUiEvent
}