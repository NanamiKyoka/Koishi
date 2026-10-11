package com.nanami.koishi.feature.tools.noise_meter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.noise_meter.engine.FrequencyWeighting
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseConfig
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseFrame
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseLevel
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseMeterEngine
import com.nanami.koishi.feature.tools.noise_meter.engine.NoiseMeterRepository
import com.nanami.koishi.feature.tools.noise_meter.engine.TimeWeighting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class RuntimeState(
    val hasPermission: Boolean,
    val isMeasuring: Boolean,
    val hasReading: Boolean,
    val errorRes: Int?
)

class NoiseMeterViewModel(
    application: Application,
    private val engine: NoiseMeterEngine,
    private val repository: NoiseMeterRepository
) : AndroidViewModel(application) {

    private val _hasPermission = MutableStateFlow(false)
    private val _isMeasuring = MutableStateFlow(false)
    private val _hasReading = MutableStateFlow(false)
    private val _frame = MutableStateFlow(NoiseFrame.Empty)
    private val _config = MutableStateFlow(NoiseConfig())
    private val _errorRes = MutableStateFlow<Int?>(null)

    private var captureJob: Job? = null
    private var sessionId = 0
    private var persistCalibrationJob: Job? = null

    private val runtimeFlow = combine(
        _hasPermission,
        _isMeasuring,
        _hasReading,
        _errorRes
    ) { permission, measuring, hasReading, errorRes ->
        RuntimeState(permission, measuring, hasReading, errorRes)
    }

    val uiState: StateFlow<NoiseMeterUiState> = combine(
        runtimeFlow,
        _frame,
        _config
    ) { runtime, frame, config ->
        val offset = config.calibrationOffsetDb
        val level = (frame.level + offset).roundToTenth()
        val hasReading = runtime.hasReading && runtime.isMeasuring
        NoiseMeterUiState(
            hasPermission = runtime.hasPermission,
            isMeasuring = runtime.isMeasuring,
            hasReading = hasReading,
            levelDb = if (hasReading) level else 0.0,
            peakDb = if (hasReading) (frame.peak + offset).roundToTenth() else 0.0,
            minimumDb = if (hasReading) (frame.minimum + offset).roundToTenth() else 0.0,
            leqDb = if (hasReading) (frame.leq + offset).roundToTenth() else 0.0,
            elapsedMs = if (hasReading) frame.elapsedMs else 0L,
            level = NoiseLevel.fromDb(if (hasReading) level else 0.0),
            frequencyWeighting = config.frequencyWeighting,
            timeWeighting = config.timeWeighting,
            calibrationOffsetDb = offset,
            errorRes = runtime.errorRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NoiseMeterUiState()
    )

    init {
        viewModelScope.launch {
            val data = repository.dataFlow.first()
            _config.value = NoiseConfig(
                frequencyWeighting = data.frequencyWeighting,
                timeWeighting = data.timeWeighting,
                calibrationOffsetDb = data.calibrationOffsetDb
            )
        }
    }

    fun onEvent(event: NoiseMeterUiEvent) {
        when (event) {
            is NoiseMeterUiEvent.OnPermissionResult -> {
                _hasPermission.value = event.granted
                if (!event.granted) _errorRes.value = R.string.noise_meter_permission_required
            }
            is NoiseMeterUiEvent.OnStart -> startCapture()
            is NoiseMeterUiEvent.OnStop -> stopCapture()
            is NoiseMeterUiEvent.OnReset -> resetReading()
            is NoiseMeterUiEvent.OnCalibrationChange -> updateCalibration(event.offsetDb)
            is NoiseMeterUiEvent.OnFrequencyWeightingChange -> updateFrequencyWeighting(event.weighting)
            is NoiseMeterUiEvent.OnTimeWeightingChange -> updateTimeWeighting(event.weighting)
            is NoiseMeterUiEvent.OnDismissError -> _errorRes.value = null
        }
    }

    private fun startCapture() {
        if (captureJob != null) return
        if (!_hasPermission.value) {
            _errorRes.value = R.string.noise_meter_permission_required
            return
        }
        _hasReading.value = false
        _frame.value = NoiseFrame.Empty
        launchCapture()
    }

    private fun launchCapture() {
        val session = ++sessionId
        _isMeasuring.value = true
        captureJob = viewModelScope.launch {
            try {
                engine.frames(_config.value).collect { frame ->
                    if (session == sessionId) {
                        _frame.value = frame
                        _hasReading.value = true
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (session == sessionId) {
                    _errorRes.value = R.string.noise_meter_microphone_error
                }
            } finally {
                if (session == sessionId) {
                    _isMeasuring.value = false
                    captureJob = null
                }
            }
        }
    }

    private fun stopCapture() {
        sessionId++
        captureJob?.cancel()
        captureJob = null
        _isMeasuring.value = false
    }

    private fun resetReading() {
        _frame.value = NoiseFrame.Empty
        _hasReading.value = _isMeasuring.value
    }

    private fun updateCalibration(offsetDb: Double) {
        val clamped = offsetDb.coerceIn(
            NoiseConfig.MIN_CALIBRATION_OFFSET_DB,
            NoiseConfig.MAX_CALIBRATION_OFFSET_DB
        )
        _config.update { it.copy(calibrationOffsetDb = clamped) }
        persistCalibrationJob?.cancel()
        persistCalibrationJob = viewModelScope.launch {
            delay(CALIBRATION_PERSIST_DELAY_MS)
            repository.updateCalibrationOffset(clamped)
        }
    }

    private fun updateFrequencyWeighting(weighting: FrequencyWeighting) {
        if (_config.value.frequencyWeighting == weighting) return
        _config.update { it.copy(frequencyWeighting = weighting) }
        viewModelScope.launch { repository.updateFrequencyWeighting(weighting) }
        if (_isMeasuring.value) restartCapture()
    }

    private fun updateTimeWeighting(weighting: TimeWeighting) {
        if (_config.value.timeWeighting == weighting) return
        _config.update { it.copy(timeWeighting = weighting) }
        viewModelScope.launch { repository.updateTimeWeighting(weighting) }
        if (_isMeasuring.value) restartCapture()
    }

    private fun restartCapture() {
        stopCapture()
        launchCapture()
    }

    override fun onCleared() {
        stopCapture()
        super.onCleared()
    }

    private fun Double.roundToTenth(): Double = kotlin.math.round(this * 10.0) / 10.0

    companion object {
        private const val CALIBRATION_PERSIST_DELAY_MS = 300L
    }
}