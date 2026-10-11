package com.nanami.koishi.feature.tools.unit_converter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitCategory
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitConverterCatalog
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitConverterEngine
import com.nanami.koishi.feature.tools.unit_converter.engine.UnitConverterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

class UnitConverterViewModel(
    application: Application,
    private val repository: UnitConverterRepository
) : AndroidViewModel(application) {

    private val initialDefaults = UnitConverterCatalog.defaults(UnitCategory.LENGTH)
    private val _uiState = MutableStateFlow(
        UnitConverterUiState(
            category = UnitCategory.LENGTH,
            fromUnit = initialDefaults.first,
            toUnit = initialDefaults.second
        )
    )
    val uiState: StateFlow<UnitConverterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val stored = repository.currentData()
            val category = UnitCategory.fromId(stored.categoryId)
            val units = UnitConverterCatalog.units(category)
            val defaults = UnitConverterCatalog.defaults(category)
            val fromUnit = units.firstOrNull { it.id == stored.fromUnitId } ?: defaults.first
            val toUnit = units.firstOrNull { it.id == stored.toUnitId } ?: defaults.second
            updateState(
                _uiState.value.copy(
                    category = category,
                    fromUnit = fromUnit,
                    toUnit = toUnit,
                    input = stored.input
                )
            )
        }
    }

    fun onEvent(event: UnitConverterUiEvent) {
        when (event) {
            is UnitConverterUiEvent.OnCategorySelected -> selectCategory(event.category)
            is UnitConverterUiEvent.OnInputChange -> changeInput(event.value)
            is UnitConverterUiEvent.OnUnitSelected -> selectUnit(event.target, event.unitId)
            UnitConverterUiEvent.OnSwapUnits -> swapUnits()
            is UnitConverterUiEvent.OnOpenPicker -> _uiState.update { it.copy(pickerTarget = event.target) }
            UnitConverterUiEvent.OnDismissPicker -> _uiState.update { it.copy(pickerTarget = null) }
            UnitConverterUiEvent.OnCopy -> copyResult()
            UnitConverterUiEvent.OnMessageShown -> _uiState.update { it.copy(messageRes = null, copiedText = "") }
        }
    }

    private fun selectCategory(category: UnitCategory) {
        val defaults = UnitConverterCatalog.defaults(category)
        updateState(
            _uiState.value.copy(
                category = category,
                fromUnit = defaults.first,
                toUnit = defaults.second,
                input = "1"
            )
        )
    }

    private fun changeInput(value: String) {
        val sanitized = sanitizeInput(value, _uiState.value.category.allowNegative)
        updateState(_uiState.value.copy(input = sanitized))
    }

    private fun selectUnit(target: UnitPickerTarget, unitId: String) {
        val state = _uiState.value
        val unit = UnitConverterCatalog.find(state.category, unitId)
        val updated = when (target) {
            UnitPickerTarget.FROM -> state.copy(fromUnit = unit, pickerTarget = null)
            UnitPickerTarget.TO -> state.copy(toUnit = unit, pickerTarget = null)
        }
        updateState(updated)
    }

    private fun swapUnits() {
        val state = _uiState.value
        val result = calculateResult(state)
        val nextInput = result?.let(UnitConverterEngine::format) ?: state.input
        updateState(
            state.copy(
                fromUnit = state.toUnit,
                toUnit = state.fromUnit,
                input = nextInput
            )
        )
    }

    private fun copyResult() {
        val resultText = _uiState.value.resultText
        if (resultText.isBlank()) return
        _uiState.update {
            it.copy(
                messageRes = R.string.unit_converter_copied,
                copiedText = "${resultText} ${it.toUnit.symbol}"
            )
        }
    }

    private fun updateState(state: UnitConverterUiState) {
        val recalculated = recalculate(state)
        _uiState.value = recalculated
        viewModelScope.launch {
            repository.updateDraft(
                categoryId = recalculated.category.id,
                fromUnitId = recalculated.fromUnit.id,
                toUnitId = recalculated.toUnit.id,
                input = recalculated.input
            )
        }
    }

    private fun calculateResult(state: UnitConverterUiState): java.math.BigDecimal? {
        val trimmed = state.input.trim()
        val value = UnitConverterEngine.parseInput(trimmed) ?: return null
        if (value.signum() < 0 && !state.category.allowNegative) return null
        return UnitConverterEngine.convert(
            value = value,
            category = state.category,
            fromUnitId = state.fromUnit.id,
            toUnitId = state.toUnit.id
        )
    }

    private fun recalculate(state: UnitConverterUiState): UnitConverterUiState {
        val trimmed = state.input.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "." || trimmed == "-.") {
            return state.copy(resultText = "", reverseText = "", inputErrorRes = null, copiedText = "")
        }
        val value = UnitConverterEngine.parseInput(trimmed)
            ?: return state.copy(
                resultText = "",
                reverseText = "",
                inputErrorRes = R.string.unit_converter_invalid_input,
                copiedText = ""
            )
        if (value.signum() < 0 && !state.category.allowNegative) {
            return state.copy(
                resultText = "",
                reverseText = "",
                inputErrorRes = R.string.unit_converter_negative_not_allowed,
                copiedText = ""
            )
        }
        val result = UnitConverterEngine.convert(
            value = value,
            category = state.category,
            fromUnitId = state.fromUnit.id,
            toUnitId = state.toUnit.id
        ) ?: return state.copy(
            resultText = "",
            reverseText = "",
            inputErrorRes = R.string.unit_converter_invalid_input,
            copiedText = ""
        )
        val reverse = UnitConverterEngine.convert(
            value = BigDecimal.ONE,
            category = state.category,
            fromUnitId = state.toUnit.id,
            toUnitId = state.fromUnit.id
        )
        return state.copy(
            resultText = UnitConverterEngine.format(result),
            reverseText = reverse?.let { UnitConverterEngine.format(it) } ?: "",
            inputErrorRes = null,
            copiedText = ""
        )
    }

    private fun sanitizeInput(value: String, allowNegative: Boolean): String {
        val builder = StringBuilder()
        var hasDecimalPoint = false
        value.forEachIndexed { index, char ->
            when {
                char.isDigit() -> builder.append(char)
                char == '-' && allowNegative && index == 0 -> builder.append(char)
                (char == '.' || char == ',') && !hasDecimalPoint -> {
                    builder.append('.')
                    hasDecimalPoint = true
                }
            }
        }
        return builder.toString().take(30)
    }
}