package com.nanami.koishi.feature.tools.currency_converter

import androidx.annotation.StringRes
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyInfo

data class CurrencyConversionRow(
    val code: String,
    val displayName: String,
    val originalName: String,
    val symbol: String,
    val flag: String,
    val isPreciousMetal: Boolean,
    val formattedAmount: String,
    val unitRateText: String,
    val isActive: Boolean
)

enum class CurrencyCategoryFilter {
    ALL,
    FIAT,
    METALS
}

data class CurrencyConverterUiState(
    val rows: List<CurrencyConversionRow> = emptyList(),
    val activeCurrencyCode: String = "CNY",
    val activeAmountInput: String = "100",
    val ratesDate: String = "",
    val lastUpdatedText: String = "",
    val isRefreshing: Boolean = false,
    val showAddCurrencySheet: Boolean = false,
    val searchQuery: String = "",
    val categoryFilter: CurrencyCategoryFilter = CurrencyCategoryFilter.ALL,
    val availableCurrencies: List<CurrencyInfo> = emptyList(),
    val selectedCurrencyCodes: Set<String> = emptySet(),
    @StringRes val userMessageRes: Int? = null,
    val userMessageArgs: List<String> = emptyList()
)

sealed interface CurrencyConverterUiEvent {
    data class OnActiveCurrencyChange(val code: String) : CurrencyConverterUiEvent
    data class OnAmountInputChange(val amountText: String) : CurrencyConverterUiEvent
    data class OnAddCurrency(val code: String) : CurrencyConverterUiEvent
    data class OnRemoveCurrency(val code: String) : CurrencyConverterUiEvent
    data object OnRefreshRates : CurrencyConverterUiEvent
    data object OnOpenAddCurrencySheet : CurrencyConverterUiEvent
    data object OnDismissAddCurrencySheet : CurrencyConverterUiEvent
    data class OnSearchQueryChange(val query: String) : CurrencyConverterUiEvent
    data class OnCategoryFilterChange(val filter: CurrencyCategoryFilter) : CurrencyConverterUiEvent
    data object OnDismissMessage : CurrencyConverterUiEvent
    data class OnCopyAmount(val text: String) : CurrencyConverterUiEvent
    data object OnClearInput : CurrencyConverterUiEvent
}
