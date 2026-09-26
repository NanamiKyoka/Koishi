package com.nanami.koishi.feature.tools.bili_cover

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.KoishiApp
import com.nanami.koishi.R
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverDownloader
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverEngines
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverException
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverParser
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverResult
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliCoverSettingsRepository
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliParseResult
import com.nanami.koishi.feature.tools.bili_cover.engine.BiliTarget
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BiliCoverViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = BiliCoverSettingsRepository((application as KoishiApp).toolStorageDao)

    private val _uiState = MutableStateFlow(BiliCoverUiState())
    val uiState: StateFlow<BiliCoverUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            settings.dataFlow.collect { stored ->
                _uiState.update { it.copy(recentQueries = stored.recentQueries) }
            }
        }
    }

    fun onEvent(event: BiliCoverUiEvent) {
        when (event) {
            is BiliCoverUiEvent.OnInputChange -> _uiState.update { it.copy(input = event.value) }
            is BiliCoverUiEvent.OnFetch -> fetch(_uiState.value.input)
            is BiliCoverUiEvent.OnSelectRecent -> {
                _uiState.update { it.copy(input = event.input) }
                fetch(event.input)
            }
            is BiliCoverUiEvent.OnClearRecent -> viewModelScope.launch { settings.clearRecent() }
            is BiliCoverUiEvent.OnSaveCover -> saveCover(event.index)
            is BiliCoverUiEvent.OnSaveAllCovers -> saveAllCovers()
            is BiliCoverUiEvent.OnShowHelp -> _uiState.update { it.copy(showHelp = event.show) }
            is BiliCoverUiEvent.OnDismissMessage -> _uiState.update {
                it.copy(userMessageRes = null, userMessageArgs = emptyList())
            }
        }
    }

    private fun fetch(rawInput: String) {
        val input = rawInput.trim()
        if (input.isEmpty()) {
            message(R.string.bili_cover_error_empty_input)
            return
        }
        if (_uiState.value.isLoading) return

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _uiState.update { it.copy(loadState = BiliCoverLoadState.Loading) }
            try {
                val result = resolve(input)
                _uiState.update { it.copy(loadState = BiliCoverLoadState.Success(result)) }
                settings.recordQuery(input)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BiliCoverException) {
                _uiState.update {
                    it.copy(loadState = BiliCoverLoadState.Error(e.toMessageRes(), e.localizedMessage))
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(loadState = BiliCoverLoadState.Error(R.string.bili_cover_error_network))
                }
            }
        }
    }

    private suspend fun resolve(input: String): BiliCoverResult {
        val parsed = BiliCoverParser.parse(input)
        val target = when (parsed) {
            is BiliParseResult.Target -> parsed.target
            is BiliParseResult.ShortLink -> expandShortLink(parsed.url)
            is BiliParseResult.Empty -> throw BiliCoverException.EmptyInput()
            is BiliParseResult.Unrecognized -> throw BiliCoverException.UnrecognizedInput()
        }
        return BiliCoverEngines.fetchCover(target)
    }

    private suspend fun expandShortLink(url: String): BiliTarget {
        val expanded = BiliCoverEngines.resolveShortLink(url)
        return (BiliCoverParser.parse(expanded) as? BiliParseResult.Target)?.target
            ?: throw BiliCoverException.UnrecognizedInput()
    }

    private fun saveCover(index: Int) {
        val result = _uiState.value.currentResult ?: return
        val cover = result.covers.getOrNull(index) ?: return
        viewModelScope.launch {
            val saved = BiliCoverDownloader.save(
                getApplication(),
                cover.url,
                buildFileName(result, index)
            )
            message(if (saved) R.string.bili_cover_saved else R.string.bili_cover_save_failed)
        }
    }

    private fun saveAllCovers() {
        val result = _uiState.value.currentResult ?: return
        if (result.covers.isEmpty()) return
        viewModelScope.launch {
            var savedCount = 0
            result.covers.forEachIndexed { index, cover ->
                val saved = BiliCoverDownloader.save(
                    getApplication(),
                    cover.url,
                    buildFileName(result, index)
                )
                if (saved) savedCount++
            }
            when (savedCount) {
                0 -> message(R.string.bili_cover_save_failed)
                result.covers.size -> message(
                    R.string.bili_cover_saved_count,
                    savedCount.toString()
                )
                else -> message(
                    R.string.bili_cover_saved_partial,
                    savedCount.toString(),
                    result.covers.size.toString()
                )
            }
        }
    }

    private fun buildFileName(result: BiliCoverResult, index: Int): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val sequence = (index + 1).toString().padStart(2, '0')
        val type = result.target.type.name.lowercase(Locale.US)
        return "bili_${type}_${result.target.identifier}_${stamp}_$sequence"
    }

    private fun message(@StringRes res: Int, vararg args: String) {
        _uiState.update { it.copy(userMessageRes = res, userMessageArgs = args.toList()) }
    }

    private fun BiliCoverException.toMessageRes(): Int = when (this) {
        is BiliCoverException.EmptyInput -> R.string.bili_cover_error_empty_input
        is BiliCoverException.UnrecognizedInput -> R.string.bili_cover_error_invalid_input
        is BiliCoverException.NotFound -> R.string.bili_cover_error_not_found
        is BiliCoverException.RiskControl -> R.string.bili_cover_error_risk
        is BiliCoverException.NoCover -> R.string.bili_cover_error_no_cover
        is BiliCoverException.ShortLink -> R.string.bili_cover_error_short_link
        is BiliCoverException.Network -> R.string.bili_cover_error_network
        is BiliCoverException.Parse -> R.string.bili_cover_error_parse
        is BiliCoverException.ServerError -> R.string.bili_cover_error_server
    }
}
