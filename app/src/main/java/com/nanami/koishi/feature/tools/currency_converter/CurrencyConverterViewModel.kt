package com.nanami.koishi.feature.tools.currency_converter

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nanami.koishi.R
import com.nanami.koishi.core.data.storage.ToolStorageDatabase
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyApi
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConstants
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConverterData
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConverterRepository
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class UserMessage(
    @StringRes val resId: Int,
    val args: List<String> = emptyList()
)

private data class SearchAndFilterState(
    val query: String,
    val filter: CurrencyCategoryFilter,
    val refreshing: Boolean,
    val showAddSheet: Boolean,
    val message: UserMessage?
)

class CurrencyConverterViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CurrencyConverterRepository = CurrencyConverterRepository(
        ToolStorageDatabase.get(application).toolStorageDao()
    )
) : AndroidViewModel(application) {

    private val isChineseLocale: Boolean
        get() = getApplication<Application>().resources.configuration.locales[0].language.startsWith("zh")

    private val _isRefreshing = MutableStateFlow(false)
    private val _showAddCurrencySheet = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _categoryFilter = MutableStateFlow(CurrencyCategoryFilter.ALL)
    private val _allCurrencies = MutableStateFlow<List<CurrencyInfo>>(CurrencyApi.getFallbackCurrencies())
    private val _userMessage = MutableStateFlow<UserMessage?>(null)

    private val _searchAndFilterFlow = combine(
        _searchQuery,
        _categoryFilter,
        _isRefreshing,
        _showAddCurrencySheet,
        _userMessage
    ) { query, filter, refreshing, showAdd, msg ->
        SearchAndFilterState(query, filter, refreshing, showAdd, msg)
    }

    val uiState: StateFlow<CurrencyConverterUiState> = combine(
        repository.dataFlow,
        _allCurrencies,
        _searchAndFilterFlow
    ) { storedData: CurrencyConverterData, currencies: List<CurrencyInfo>, sfState: SearchAndFilterState ->
        val isZh = isChineseLocale
        val selectedSet = storedData.selectedCurrencies.toSet()
        val activeCode = storedData.activeCurrencyCode
        val activeInput = storedData.activeAmountText
        val activeAmount = activeInput.toDoubleOrNull() ?: 0.0
        val rates = storedData.rates
        val activeRate = rates[activeCode] ?: 1.0

        val currencyMap = currencies.associateBy { it.code }

        val rows = storedData.selectedCurrencies.map { code ->
            val info = currencyMap[code] ?: CurrencyInfo(
                code = code,
                name = code,
                symbol = CurrencyConstants.resolveSymbol(code, null),
                isPreciousMetal = CurrencyConstants.isMetal(code)
            )
            val displayName = CurrencyConstants.getDisplayName(code, info.name, isZh)
            val symbol = info.symbol ?: CurrencyConstants.resolveSymbol(code, null)
            val isCurrentActive = code.equals(activeCode, ignoreCase = true)
            val targetRate = rates[code] ?: 1.0

            val formattedAmount = if (isCurrentActive) {
                activeInput
            } else {
                val converted = if (activeRate > 0.0) activeAmount * (targetRate / activeRate) else 0.0
                formatAmount(converted, info.isPreciousMetal)
            }

            val unitRateText = buildUnitRateText(
                activeCode = activeCode,
                targetCode = code,
                activeRate = activeRate,
                targetRate = targetRate,
                isTargetMetal = info.isPreciousMetal,
                isZh = isZh
            )

            CurrencyConversionRow(
                code = code,
                displayName = displayName,
                originalName = info.name,
                symbol = symbol,
                flag = info.flag,
                isPreciousMetal = info.isPreciousMetal,
                formattedAmount = formattedAmount,
                unitRateText = unitRateText,
                isActive = isCurrentActive
            )
        }

        val filteredCurrencies = currencies.filter { currency ->
            val matchesFilter = when (sfState.filter) {
                CurrencyCategoryFilter.ALL -> true
                CurrencyCategoryFilter.FIAT -> !currency.isPreciousMetal
                CurrencyCategoryFilter.METALS -> currency.isPreciousMetal
            }
            val matchesQuery = sfState.query.isBlank() ||
                    currency.code.contains(sfState.query, ignoreCase = true) ||
                    currency.name.contains(sfState.query, ignoreCase = true) ||
                    CurrencyConstants.getDisplayName(currency.code, currency.name, true).contains(sfState.query, ignoreCase = true) ||
                    (currency.symbol != null && currency.symbol.contains(sfState.query, ignoreCase = true))

            matchesFilter && matchesQuery
        }

        CurrencyConverterUiState(
            rows = rows,
            activeCurrencyCode = activeCode,
            activeAmountInput = activeInput,
            ratesDate = storedData.ratesDate,
            lastUpdatedText = formatTimestamp(storedData.lastUpdatedTimestamp),
            isRefreshing = sfState.refreshing,
            showAddCurrencySheet = sfState.showAddSheet,
            searchQuery = sfState.query,
            categoryFilter = sfState.filter,
            availableCurrencies = filteredCurrencies,
            selectedCurrencyCodes = selectedSet,
            userMessageRes = sfState.message?.resId,
            userMessageArgs = sfState.message?.args.orEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CurrencyConverterUiState()
    )

    init {
        loadCurrenciesCatalog()
        checkAndRefreshRatesOnStartup()
    }

    private fun loadCurrenciesCatalog() {
        viewModelScope.launch {
            try {
                val fetched = CurrencyApi.fetchCurrencies()
                if (fetched.isNotEmpty()) {
                    _allCurrencies.value = fetched
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun checkAndRefreshRatesOnStartup() {
        viewModelScope.launch {
            val current = repository.currentData()
            val hourInMillis = 3600_000L
            if (current.lastUpdatedTimestamp == 0L || System.currentTimeMillis() - current.lastUpdatedTimestamp > hourInMillis * 2) {
                refreshRatesInternal(silent = true)
            }
        }
    }

    fun onEvent(event: CurrencyConverterUiEvent) {
        when (event) {
            is CurrencyConverterUiEvent.OnActiveCurrencyChange -> {
                viewModelScope.launch {
                    val current = repository.currentData()
                    if (current.activeCurrencyCode != event.code) {
                        val activeRate = current.rates[current.activeCurrencyCode] ?: 1.0
                        val targetRate = current.rates[event.code] ?: 1.0
                        val currentAmount = current.activeAmountText.toDoubleOrNull() ?: 0.0
                        val newAmount = if (activeRate > 0.0) currentAmount * (targetRate / activeRate) else 0.0
                        val formatted = formatAmount(newAmount, CurrencyConstants.isMetal(event.code)).replace(",", "")
                        repository.updateActiveInput(event.code, formatted)
                    }
                }
            }

            is CurrencyConverterUiEvent.OnAmountInputChange -> {
                val sanitized = sanitizeNumberInput(event.amountText)
                viewModelScope.launch {
                    val current = repository.currentData()
                    repository.updateActiveInput(current.activeCurrencyCode, sanitized)
                }
            }

            is CurrencyConverterUiEvent.OnAddCurrency -> {
                viewModelScope.launch {
                    repository.addCurrency(event.code)
                    _userMessage.value = UserMessage(R.string.currency_converter_added_success, listOf(event.code))
                }
            }

            is CurrencyConverterUiEvent.OnRemoveCurrency -> {
                viewModelScope.launch {
                    val current = repository.currentData()
                    if (current.selectedCurrencies.size <= 2) {
                        _userMessage.value = UserMessage(R.string.currency_converter_min_limit_hint)
                    } else {
                        repository.removeCurrency(event.code)
                        _userMessage.value = UserMessage(R.string.currency_converter_removed_success, listOf(event.code))
                    }
                }
            }

            is CurrencyConverterUiEvent.OnRefreshRates -> {
                viewModelScope.launch {
                    refreshRatesInternal(silent = false)
                }
            }

            is CurrencyConverterUiEvent.OnOpenAddCurrencySheet -> {
                _searchQuery.value = ""
                _categoryFilter.value = CurrencyCategoryFilter.ALL
                _showAddCurrencySheet.value = true
            }

            is CurrencyConverterUiEvent.OnDismissAddCurrencySheet -> {
                _showAddCurrencySheet.value = false
            }

            is CurrencyConverterUiEvent.OnSearchQueryChange -> {
                _searchQuery.value = event.query
            }

            is CurrencyConverterUiEvent.OnCategoryFilterChange -> {
                _categoryFilter.value = event.filter
            }

            is CurrencyConverterUiEvent.OnDismissMessage -> {
                _userMessage.value = null
            }

            is CurrencyConverterUiEvent.OnCopyAmount -> {
                _userMessage.value = UserMessage(R.string.currency_converter_amount_copied)
            }

            is CurrencyConverterUiEvent.OnClearInput -> {
                viewModelScope.launch {
                    val current = repository.currentData()
                    repository.updateActiveInput(current.activeCurrencyCode, "0")
                }
            }
        }
    }

    private suspend fun refreshRatesInternal(silent: Boolean) {
        _isRefreshing.value = true
        try {
            val (date, rates) = CurrencyApi.fetchRates("USD")
            repository.updateRates(date, rates)
            if (!silent) {
                _userMessage.value = UserMessage(R.string.currency_converter_refresh_success)
            }
        } catch (_: Exception) {
            if (!silent) {
                _userMessage.value = UserMessage(R.string.currency_converter_refresh_failed)
            }
        } finally {
            _isRefreshing.value = false
        }
    }

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
        return if (filtered.length > 15) filtered.take(15) else filtered
    }

    private fun formatAmount(amount: Double, isMetal: Boolean): String {
        if (amount == 0.0) return "0"
        return try {
            val maxDecimals = if (isMetal) 6 else if (amount >= 100.0) 2 else 4
            val symbols = DecimalFormatSymbols(Locale.US).apply {
                groupingSeparator = ','
                decimalSeparator = '.'
            }
            val pattern = if (amount >= 1.0) "#,##0.00" else "0.0000"
            val df = DecimalFormat(pattern, symbols).apply {
                maximumFractionDigits = maxDecimals
                minimumFractionDigits = if (amount >= 1.0) 2 else 4
            }
            val bigDecimal = BigDecimal(amount).setScale(maxDecimals, RoundingMode.HALF_UP)
            df.format(bigDecimal)
        } catch (_: Exception) {
            amount.toString()
        }
    }

    private fun buildUnitRateText(
        activeCode: String,
        targetCode: String,
        activeRate: Double,
        targetRate: Double,
        isTargetMetal: Boolean,
        isZh: Boolean
    ): String {
        if (activeCode.equals(targetCode, ignoreCase = true) || activeRate <= 0.0 || targetRate <= 0.0) {
            return ""
        }

        return if (isTargetMetal) {
            val pricePerOunce = activeRate / targetRate
            val formatted = formatAmount(pricePerOunce, false)
            val unitName = if (isZh) "盎司" else "oz t"
            "1 $unitName ≈ $formatted $activeCode"
        } else {
            val unitRatio = targetRate / activeRate
            val formatted = formatAmount(unitRatio, false)
            "1 $activeCode ≈ $formatted $targetCode"
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60000
        return when {
            minutes < 1 -> if (isChineseLocale) "刚刚" else "Just now"
            minutes < 60 -> if (isChineseLocale) "${minutes}分钟前" else "${minutes}m ago"
            else -> {
                val hours = minutes / 60
                if (hours < 24) {
                    if (isChineseLocale) "${hours}小时前" else "${hours}h ago"
                } else {
                    val days = hours / 24
                    if (isChineseLocale) "${days}天前" else "${days}d ago"
                }
            }
        }
    }
}
