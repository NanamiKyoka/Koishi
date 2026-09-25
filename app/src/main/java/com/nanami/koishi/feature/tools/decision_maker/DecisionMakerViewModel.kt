package com.nanami.koishi.feature.tools.decision_maker

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionIds
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionMode
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionPhase
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionPreferences
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionRepository
import com.nanami.koishi.feature.tools.decision_maker.engine.DecisionTopic
import com.nanami.koishi.feature.tools.decision_maker.engine.TopicImportResult
import com.nanami.koishi.feature.tools.decision_maker.engine.WeightedPicker
import com.nanami.koishi.feature.tools.decision_maker.engine.playableOptions
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DecisionMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val preferences = DecisionPreferences(application)
    private val repository = DecisionRepository(application, preferences)

    private val _uiState = MutableStateFlow(
        DecisionMakerUiState(
            mode = preferences.mode,
            hapticsEnabled = preferences.hapticsEnabled
        )
    )
    val uiState: StateFlow<DecisionMakerUiState> = _uiState.asStateFlow()

    private var energyJob: Job? = null
    private var animationToken = 0L

    init {
        viewModelScope.launch {
            repository.topics.collect { topics -> applyTopics(topics) }
        }
    }

    fun onEvent(event: DecisionMakerUiEvent) {
        when (event) {
            is DecisionMakerUiEvent.OnSelectMode -> selectMode(event.mode)
            is DecisionMakerUiEvent.OnStartSpin -> startSpin()
            is DecisionMakerUiEvent.OnSpinFinished -> settle(DecisionPhase.RESULT)
            is DecisionMakerUiEvent.OnShakeProgress -> accumulateEnergy(event.delta)
            is DecisionMakerUiEvent.OnShakeImpulse -> accumulateEnergy(SHAKE_IMPULSE_ENERGY)
            is DecisionMakerUiEvent.OnDrawFinished -> settle(DecisionPhase.RESULT)
            is DecisionMakerUiEvent.OnResetRound -> resetRound()
            is DecisionMakerUiEvent.OnSelectTopic -> selectTopic(event.topicId)
            is DecisionMakerUiEvent.OnShowTopicManager -> {
                _uiState.update { it.copy(showTopicManager = event.show) }
            }
            is DecisionMakerUiEvent.OnShowOptionEditor -> {
                _uiState.update { it.copy(showOptionEditor = event.show) }
            }
            is DecisionMakerUiEvent.OnShowHelpDialog -> {
                _uiState.update { it.copy(showHelpDialog = event.show) }
            }
            is DecisionMakerUiEvent.OnSaveTopic -> saveTopic(event.topic)
            is DecisionMakerUiEvent.OnDeleteTopic -> deleteTopic(event.topicId)
            is DecisionMakerUiEvent.OnRestoreBuiltInPresets -> restoreBuiltIns()
            is DecisionMakerUiEvent.OnCreateTopic -> createTopic()
            is DecisionMakerUiEvent.OnImportFrom -> importFrom(event.uri)
            is DecisionMakerUiEvent.OnExportTo -> exportTo(event.uri)
            is DecisionMakerUiEvent.OnHapticsEnabledChange -> setHapticsEnabled(event.enabled)
            is DecisionMakerUiEvent.OnDismissMessage -> {
                _uiState.update { it.copy(userMessageRes = null, userMessageArg = null) }
            }
        }
    }

    private fun applyTopics(topics: List<DecisionTopic>) {
        _uiState.update { state ->
            val targetId = state.currentTopic?.id ?: preferences.selectedTopicId
            val current = topics.firstOrNull { it.id == targetId } ?: topics.firstOrNull()
            if (current != null) preferences.selectedTopicId = current.id
            state.copy(
                topics = topics,
                currentTopic = current,
                phase = DecisionPhase.IDLE,
                shakeEnergy = 0f,
                result = null
            )
        }
        stopEnergyTicker()
    }

    private fun selectMode(mode: DecisionMode) {
        preferences.mode = mode
        resetRound()
        _uiState.update { it.copy(mode = mode) }
    }

    private fun selectTopic(topicId: String) {
        val topic = repository.findTopic(topicId) ?: return
        preferences.selectedTopicId = topicId
        stopEnergyTicker()
        _uiState.update {
            it.copy(
                currentTopic = topic,
                showTopicManager = false,
                phase = DecisionPhase.IDLE,
                shakeEnergy = 0f,
                result = null
            )
        }
    }

    private fun startSpin() {
        val state = _uiState.value
        if (state.isBusy) return
        if (!state.canPlay) {
            message(R.string.decision_need_two_options)
            return
        }
        launchDraw(DecisionPhase.SPINNING)
    }

    private fun accumulateEnergy(delta: Float) {
        val state = _uiState.value
        if (state.mode != DecisionMode.FORTUNE_STICK) return
        if (state.phase == DecisionPhase.SPINNING || state.phase == DecisionPhase.DRAWING) return
        if (!state.canPlay) return

        if (state.phase == DecisionPhase.RESULT) {
            stopEnergyTicker()
            _uiState.update {
                it.copy(phase = DecisionPhase.IDLE, shakeEnergy = 0f, result = null)
            }
        }

        val next = (_uiState.value.shakeEnergy + delta).coerceIn(0f, 1f)
        if (next >= 1f) {
            launchDraw(DecisionPhase.DRAWING)
            return
        }
        _uiState.update { it.copy(shakeEnergy = next, phase = DecisionPhase.SHAKING) }
        startEnergyTicker()
    }

    private fun launchDraw(phase: DecisionPhase) {
        val state = _uiState.value
        val topic = state.currentTopic ?: return
        val picked = WeightedPicker.pick(topic.playableOptions)
        if (picked == null) {
            message(R.string.decision_need_two_options)
            return
        }

        stopEnergyTicker()
        animationToken++
        _uiState.update {
            it.copy(
                phase = phase,
                result = picked,
                shakeEnergy = if (phase == DecisionPhase.DRAWING) 1f else it.shakeEnergy,
                animationToken = animationToken
            )
        }
    }

    private fun settle(target: DecisionPhase) {
        _uiState.update { state ->
            if (state.isBusy && state.result != null) {
                state.copy(phase = target, shakeEnergy = 0f)
            } else {
                state
            }
        }
    }

    private fun resetRound() {
        stopEnergyTicker()
        _uiState.update {
            it.copy(
                phase = DecisionPhase.IDLE,
                shakeEnergy = 0f,
                result = null,
                animationToken = 0L
            )
        }
    }

    private fun saveTopic(topic: DecisionTopic) {
        val saved = repository.saveTopic(topic)
        _uiState.update {
            it.copy(
                currentTopic = saved,
                showOptionEditor = false,
                phase = DecisionPhase.IDLE,
                result = null
            )
        }
    }

    private fun deleteTopic(topicId: String) {
        val target = repository.findTopic(topicId) ?: return
        repository.deleteTopic(topicId)
        message(R.string.decision_topic_deleted, target.title)
    }

    private fun restoreBuiltIns() {
        repository.restoreBuiltIns()
        message(R.string.decision_builtin_restored_all)
    }

    private fun createTopic() {
        val topic = DecisionTopic(
            id = DecisionIds.newTopicId(),
            title = getApplication<Application>().getString(R.string.decision_new_topic_title)
        )
        repository.saveTopic(topic)
        _uiState.update {
            it.copy(
                currentTopic = repository.findTopic(topic.id),
                showTopicManager = false,
                showOptionEditor = true
            )
        }
    }

    private fun importFrom(uri: Uri) {
        viewModelScope.launch {
            val raw = repository.readFrom(uri)
            val result = raw?.let { repository.importJson(it) }
            val messageRes = when (result) {
                is TopicImportResult.Success -> R.string.decision_import_success
                is TopicImportResult.Empty -> R.string.decision_import_empty
                is TopicImportResult.Malformed -> R.string.decision_import_failed
                null -> R.string.decision_import_failed
            }
            _uiState.update {
                it.copy(showTopicManager = false, userMessageRes = messageRes, userMessageArg = null)
            }
        }
    }

    private fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val success = repository.writeTo(uri)
            message(
                if (success) R.string.decision_export_success else R.string.decision_export_failed
            )
        }
    }

    private fun setHapticsEnabled(enabled: Boolean) {
        preferences.hapticsEnabled = enabled
        _uiState.update { it.copy(hapticsEnabled = enabled) }
    }

    private fun message(@StringRes res: Int, arg: String? = null) {
        _uiState.update { it.copy(userMessageRes = res, userMessageArg = arg) }
    }

    private fun startEnergyTicker() {
        if (energyJob?.isActive == true) return
        energyJob = viewModelScope.launch {
            while (isActive) {
                delay(ENERGY_TICK_MS)
                val state = _uiState.value
                if (state.phase != DecisionPhase.SHAKING) break
                val decayed = (state.shakeEnergy - ENERGY_DECAY_STEP).coerceAtLeast(0f)
                _uiState.update {
                    it.copy(
                        shakeEnergy = decayed,
                        phase = if (decayed <= 0f) DecisionPhase.IDLE else DecisionPhase.SHAKING
                    )
                }
                if (decayed <= 0f) break
            }
        }
    }

    private fun stopEnergyTicker() {
        energyJob?.cancel()
        energyJob = null
    }

    override fun onCleared() {
        stopEnergyTicker()
        super.onCleared()
    }

    private companion object {
        const val SHAKE_IMPULSE_ENERGY = 0.085f
        const val ENERGY_DECAY_STEP = 0.006f
        const val ENERGY_TICK_MS = 120L
    }
}
