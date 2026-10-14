package com.nanami.koishi.feature.tools.spirit_level

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.spirit_level.engine.LevelMode
import com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelEngine
import com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelFrame
import com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelMath
import com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelSettings
import com.nanami.koishi.feature.tools.spirit_level.engine.SpiritLevelSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SpiritLevelViewModel(
    application: Application,
    private val engine: SpiritLevelEngine,
    private val repository: SpiritLevelSettingsRepository
) : AndroidViewModel(application) {

    private val _frame = MutableStateFlow(SpiritLevelFrame())
    private val _settings = MutableStateFlow(SpiritLevelSettings())
    private val _isHeld = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)
    private var sensorJob: Job? = null

    private val isSupported = engine.isSupported(application)

    val uiState: StateFlow<SpiritLevelUiState> = combine(
        _frame,
        _settings,
        _isHeld,
        _errorRes
    ) { frame, settings, held, errorRes ->
        val calibrated = SpiritLevelMath.applyCalibration(frame, settings)
        SpiritLevelUiState(
            isSupported = isSupported,
            isHeld = held,
            mode = settings.mode,
            frame = calibrated,
            hasCalibration = settings.pitchOffsetDeg != 0.0 ||
                settings.rollOffsetDeg != 0.0 ||
                settings.tiltOffsetDeg != 0.0,
            errorRes = errorRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SpiritLevelUiState(isSupported = isSupported)
    )

    init {
        viewModelScope.launch {
            repository.dataFlow.collect { _settings.value = it }
        }
    }

    fun onEvent(event: SpiritLevelUiEvent) {
        when (event) {
            SpiritLevelUiEvent.OnStart -> startSensors()
            SpiritLevelUiEvent.OnStop -> stopSensors()
            SpiritLevelUiEvent.OnToggleHold -> setHeld(!_isHeld.value)
            is SpiritLevelUiEvent.OnModeChange -> updateMode(event.mode)
            SpiritLevelUiEvent.OnCalibrate -> calibrate()
            SpiritLevelUiEvent.OnResetCalibration -> resetCalibration()
            SpiritLevelUiEvent.OnDismissError -> _errorRes.value = null
        }
    }

    private fun startSensors() {
        if (_isHeld.value || sensorJob != null) return
        if (!isSupported) {
            _errorRes.value = R.string.spirit_level_sensor_unavailable
            return
        }

        sensorJob = viewModelScope.launch {
            try {
                engine.frames(getApplication()).collect { _frame.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _errorRes.value = R.string.spirit_level_sensor_error
            } finally {
                sensorJob = null
            }
        }
    }

    private fun stopSensors() {
        sensorJob?.cancel()
        sensorJob = null
    }

    private fun setHeld(held: Boolean) {
        _isHeld.value = held
        if (held) {
            stopSensors()
        } else {
            startSensors()
        }
    }

    private fun updateMode(mode: LevelMode) {
        if (_settings.value.mode == mode) return
        _settings.value = _settings.value.copy(mode = mode)
        viewModelScope.launch { repository.updateMode(mode) }
    }

    private fun calibrate() {
        val frame = _frame.value
        _settings.value = _settings.value.copy(
            pitchOffsetDeg = frame.pitchDeg,
            rollOffsetDeg = frame.rollDeg,
            tiltOffsetDeg = frame.tiltDeg
        )
        viewModelScope.launch { repository.updateCalibration(frame) }
    }

    private fun resetCalibration() {
        _settings.value = _settings.value.copy(
            pitchOffsetDeg = 0.0,
            rollOffsetDeg = 0.0,
            tiltOffsetDeg = 0.0
        )
        viewModelScope.launch { repository.resetCalibration() }
    }

    override fun onCleared() {
        stopSensors()
        super.onCleared()
    }
}
