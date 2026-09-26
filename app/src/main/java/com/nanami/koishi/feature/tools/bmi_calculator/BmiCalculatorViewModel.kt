package com.nanami.koishi.feature.tools.bmi_calculator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.data.storage.ToolStorageDatabase
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCalculator
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiCalculatorData
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiRecord
import com.nanami.koishi.feature.tools.bmi_calculator.engine.BmiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class BmiInputState(
    val height: String,
    val weight: String,
    val heightError: Boolean,
    val weightError: Boolean
)

data class BmiPanelState(
    val selectedRecordId: String?,
    val showClearConfirm: Boolean,
    val messageRes: Int?,
    val messageArgs: List<String>,
    val messageHasUndo: Boolean
)

class BmiCalculatorViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: BmiRepository = BmiRepository(
        ToolStorageDatabase.get(application).toolStorageDao()
    )
) : AndroidViewModel(application) {

    private val _heightInput = MutableStateFlow("")
    private val _weightInput = MutableStateFlow("")
    private val _heightError = MutableStateFlow(false)
    private val _weightError = MutableStateFlow(false)
    private val _selectedRecordId = MutableStateFlow<String?>(null)
    private val _showClearConfirm = MutableStateFlow(false)
    private val _messageRes = MutableStateFlow<Int?>(null)
    private val _messageArgs = MutableStateFlow<List<String>>(emptyList())
    private val _messageHasUndo = MutableStateFlow(false)

    private var deletedRecords: List<BmiRecord> = emptyList()

    private val isChineseLocale: Boolean
        get() = getApplication<Application>().resources.configuration.locales[0]
            .language.startsWith("zh")

    private val inputFlow = combine(
        _heightInput,
        _weightInput,
        _heightError,
        _weightError
    ) { height, weight, heightError, weightError ->
        BmiInputState(height, weight, heightError, weightError)
    }

    private val panelFlow = combine(
        _selectedRecordId,
        _showClearConfirm,
        _messageRes,
        _messageArgs,
        _messageHasUndo
    ) { selectedId, showClear, messageRes, messageArgs, hasUndo ->
        BmiPanelState(
            selectedRecordId = selectedId,
            showClearConfirm = showClear,
            messageRes = messageRes,
            messageArgs = messageArgs,
            messageHasUndo = hasUndo
        )
    }

    val uiState: StateFlow<BmiCalculatorUiState> = combine(
        repository.dataFlow,
        inputFlow,
        panelFlow
    ) { data: BmiCalculatorData, input: BmiInputState, panel: BmiPanelState ->
        val isZh = isChineseLocale
        val height = parseInput(input.height)?.takeIf { BmiCalculator.isValidHeight(it) }
        val weight = parseInput(input.weight)?.takeIf { BmiCalculator.isValidWeight(it) }

        val rows = buildRecordRows(data.records, isZh)
        val previewBmi = if (height != null && weight != null) {
            BmiCalculator.calculateBmi(height, weight)
        } else {
            null
        }

        val healthyRange = height?.let { BmiCalculator.healthyWeightRange(it) }
        val deviation = if (height != null && weight != null) {
            BmiCalculator.healthyWeightDeviation(height, weight)
        } else {
            null
        }

        BmiCalculatorUiState(
            heightInput = input.height,
            weightInput = input.weight,
            heightError = input.heightError,
            weightError = input.weightError,
            previewBmiText = previewBmi?.let { BmiCalculator.formatBmi(it) }.orEmpty(),
            previewCategory = previewBmi?.let { BmiCalculator.categoryOf(it) },
            gaugeFraction = previewBmi?.let { BmiCalculator.gaugeFraction(it) } ?: 0f,
            healthyWeightMinText = healthyRange?.let { BmiCalculator.formatValue(it.start) }.orEmpty(),
            healthyWeightMaxText = healthyRange
                ?.let { BmiCalculator.formatValue(it.endInclusive) }
                .orEmpty(),
            healthyHintRes = deviation?.let { resolveHealthyHint(it) },
            healthyHintArgs = deviation?.let { resolveHealthyHintArgs(it) }.orEmpty(),
            trendRecords = rows,
            historyRecords = rows.asReversed(),
            selectedRecordId = panel.selectedRecordId,
            selectedRecord = rows.find { it.id == panel.selectedRecordId },
            showClearConfirm = panel.showClearConfirm,
            messageRes = panel.messageRes,
            messageArgs = panel.messageArgs,
            messageHasUndo = panel.messageHasUndo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BmiCalculatorUiState()
    )

    init {
        viewModelScope.launch {
            val stored = repository.currentData()
            if (_heightInput.value.isBlank()) _heightInput.value = stored.draftHeightCm
            if (_weightInput.value.isBlank()) _weightInput.value = stored.draftWeightKg
        }
    }

    fun onEvent(event: BmiCalculatorUiEvent) {
        when (event) {
            is BmiCalculatorUiEvent.OnHeightChange -> {
                _heightInput.value = sanitizeNumberInput(event.value)
                _heightError.value = false
            }

            is BmiCalculatorUiEvent.OnWeightChange -> {
                _weightInput.value = sanitizeNumberInput(event.value)
                _weightError.value = false
            }

            is BmiCalculatorUiEvent.OnSaveRecord -> saveRecord()

            is BmiCalculatorUiEvent.OnSelectRecord -> {
                _selectedRecordId.value = if (_selectedRecordId.value == event.recordId) {
                    null
                } else {
                    event.recordId
                }
            }

            is BmiCalculatorUiEvent.OnClearSelection -> {
                _selectedRecordId.value = null
            }

            is BmiCalculatorUiEvent.OnDeleteRecord -> deleteRecord(event.recordId)

            is BmiCalculatorUiEvent.OnUndoDelete -> undoDelete()

            is BmiCalculatorUiEvent.OnRequestClearAll -> {
                _showClearConfirm.value = true
            }

            is BmiCalculatorUiEvent.OnDismissClearAll -> {
                _showClearConfirm.value = false
            }

            is BmiCalculatorUiEvent.OnConfirmClearAll -> {
                _showClearConfirm.value = false
                viewModelScope.launch {
                    repository.clearRecords()
                    _selectedRecordId.value = null
                    deletedRecords = emptyList()
                    showMessage(R.string.bmi_cleared_done)
                }
            }

            is BmiCalculatorUiEvent.OnDismissMessage -> clearMessage()
        }
    }

    private fun saveRecord() {
        val height = parseInput(_heightInput.value)
        val weight = parseInput(_weightInput.value)
        val heightValid = height != null && BmiCalculator.isValidHeight(height)
        val weightValid = weight != null && BmiCalculator.isValidWeight(weight)

        _heightError.value = !heightValid
        _weightError.value = !weightValid

        when {
            height == null || !BmiCalculator.isValidHeight(height) ->
                showMessage(R.string.bmi_invalid_height)

            weight == null || !BmiCalculator.isValidWeight(weight) ->
                showMessage(R.string.bmi_invalid_weight)

            else -> viewModelScope.launch {
                repository.addRecord(height, weight)
                showMessage(R.string.bmi_saved_done)
            }
        }
    }

    private fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            val current = repository.currentData()
            val target = current.records.find { it.id == recordId } ?: return@launch
            deletedRecords = listOf(target)
            repository.removeRecord(recordId)
            if (_selectedRecordId.value == recordId) _selectedRecordId.value = null
            showMessage(R.string.bmi_delete_done, hasUndo = true)
        }
    }

    private fun undoDelete() {
        val pending = deletedRecords
        if (pending.isEmpty()) return
        viewModelScope.launch {
            pending.forEach { repository.restoreRecord(it) }
            deletedRecords = emptyList()
            showMessage(R.string.bmi_undo_done)
        }
    }

    private fun showMessage(resId: Int, args: List<String> = emptyList(), hasUndo: Boolean = false) {
        _messageArgs.value = args
        _messageHasUndo.value = hasUndo
        _messageRes.value = resId
    }

    private fun clearMessage() {
        _messageRes.value = null
        _messageArgs.value = emptyList()
        _messageHasUndo.value = false
    }

    private fun buildRecordRows(records: List<BmiRecord>, isZh: Boolean): List<BmiRecordUi> {
        val ordered = records.sortedBy { it.timestamp }
        return ordered.mapIndexed { index, record ->
            val previous = ordered.getOrNull(index - 1)
            val deltaValue = previous?.let { abs(record.bmi - it.bmi) } ?: 0.0
            val direction = when {
                previous == null || deltaValue < 0.05 -> BmiTrendDirection.FLAT
                record.bmi > previous.bmi -> BmiTrendDirection.UP
                else -> BmiTrendDirection.DOWN
            }

            BmiRecordUi(
                id = record.id,
                bmi = record.bmi,
                bmiText = BmiCalculator.formatBmi(record.bmi),
                heightText = BmiCalculator.formatValue(record.heightCm),
                weightText = BmiCalculator.formatValue(record.weightKg),
                timeText = formatTime(record.timestamp, isZh),
                axisLabel = formatAxisLabel(record.timestamp, isZh),
                category = record.category,
                deltaDirection = direction,
                deltaText = if (previous == null) "" else BmiCalculator.formatBmi(deltaValue),
                isLatest = index == ordered.lastIndex
            )
        }
    }

    private fun resolveHealthyHint(deviation: Double): Int = when {
        abs(deviation) < 0.05 -> R.string.bmi_healthy_hint_normal
        deviation < 0 -> R.string.bmi_healthy_hint_gain
        else -> R.string.bmi_healthy_hint_lose
    }

    private fun resolveHealthyHintArgs(deviation: Double): List<String> {
        if (abs(deviation) < 0.05) return emptyList()
        return listOf(BmiCalculator.formatValue(abs(deviation)))
    }

    private fun formatTime(timestamp: Long, isZh: Boolean): String {
        val pattern = if (isZh) "M月d日 HH:mm" else "MMM d, HH:mm"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }

    private fun formatAxisLabel(timestamp: Long, isZh: Boolean): String {
        val pattern = if (isZh) "M/d" else "MMM d"
        return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
    }

    private fun parseInput(raw: String): Double? =
        raw.trim().toDoubleOrNull()?.takeIf { it > 0.0 }

    private fun sanitizeNumberInput(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""
        val filtered = buildString {
            var hasDot = false
            for (char in trimmed) {
                if (char.isDigit()) {
                    append(char)
                } else if (char == '.' && !hasDot) {
                    append(char)
                    hasDot = true
                }
            }
        }
        return filtered.take(6)
    }
}
