package com.nanami.koishi.feature.tools.compass

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.compass.engine.CompassEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CompassViewModel(
    application: Application,
    private val engine: CompassEngine
) : AndroidViewModel(application) {

    private val _frame = MutableStateFlow(com.nanami.koishi.feature.tools.compass.engine.CompassFrame())
    private val _isHeld = MutableStateFlow(false)
    private val _errorRes = MutableStateFlow<Int?>(null)
    private var sensorJob: Job? = null

    private val isSupported = engine.isSupported(application)

    val uiState: StateFlow<CompassUiState> = combine(
        _frame,
        _isHeld,
        _errorRes
    ) { frame, held, errorRes ->
        CompassUiState(
            isSupported = isSupported,
            isHeld = held,
            frame = frame,
            errorRes = errorRes
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CompassUiState(isSupported = isSupported)
    )

    fun onEvent(event: CompassUiEvent) {
        when (event) {
            CompassUiEvent.OnStart -> startSensors()
            CompassUiEvent.OnStop -> stopSensors()
            CompassUiEvent.OnToggleHold -> setHeld(!_isHeld.value)
            CompassUiEvent.OnDismissError -> _errorRes.value = null
        }
    }

    private fun startSensors() {
        if (_isHeld.value || sensorJob != null) return
        if (!isSupported) {
            _errorRes.value = R.string.compass_sensor_unavailable
            return
        }

        sensorJob = viewModelScope.launch {
            try {
                engine.frames(getApplication()).collect { _frame.value = it }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _errorRes.value = R.string.compass_sensor_error
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

    override fun onCleared() {
        stopSensors()
        super.onCleared()
    }
}
