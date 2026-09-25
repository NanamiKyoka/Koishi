package com.nanami.koishi.feature.tools.currency_converter.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao

class CurrencyConverterRepository(
    dao: ToolStorageDao
) : BaseToolRepository<CurrencyConverterData>(
    toolId = TOOL_ID,
    serializer = CurrencyConverterData.serializer(),
    dao = dao,
    defaultData = CurrencyConverterData()
) {

    suspend fun addCurrency(code: String): CurrencyConverterData = updateData { current ->
        val upper = code.uppercase()
        if (current.selectedCurrencies.contains(upper)) {
            current
        } else {
            current.copy(selectedCurrencies = current.selectedCurrencies + upper)
        }
    }

    suspend fun removeCurrency(code: String): CurrencyConverterData = updateData { current ->
        val upper = code.uppercase()
        if (current.selectedCurrencies.size <= 2) {
            current
        } else {
            val updatedList = current.selectedCurrencies.filterNot { it == upper }
            val newActive = if (current.activeCurrencyCode == upper) {
                updatedList.firstOrNull() ?: "USD"
            } else {
                current.activeCurrencyCode
            }
            current.copy(
                selectedCurrencies = updatedList,
                activeCurrencyCode = newActive
            )
        }
    }

    suspend fun updateRates(date: String, newRates: Map<String, Double>): CurrencyConverterData = updateData { current ->
        current.copy(
            rates = current.rates + newRates,
            ratesDate = date,
            lastUpdatedTimestamp = System.currentTimeMillis()
        )
    }

    suspend fun updateActiveInput(code: String, amountText: String): CurrencyConverterData = updateData { current ->
        current.copy(
            activeCurrencyCode = code,
            activeAmountText = amountText
        )
    }

    companion object {
        const val TOOL_ID = "currency_converter"
    }
}
