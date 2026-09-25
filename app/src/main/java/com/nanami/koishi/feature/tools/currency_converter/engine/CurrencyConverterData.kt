package com.nanami.koishi.feature.tools.currency_converter.engine

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyConverterData(
    val selectedCurrencies: List<String> = CurrencyConstants.DEFAULT_SELECTED_CURRENCIES,
    val rates: Map<String, Double> = defaultFallbackRates(),
    val ratesDate: String = "2026-09-25",
    val lastUpdatedTimestamp: Long = 0L,
    val activeCurrencyCode: String = "CNY",
    val activeAmountText: String = "100"
)

fun defaultFallbackRates(): Map<String, Double> = mapOf(
    "USD" to 1.0,
    "CNY" to 6.7095,
    "EUR" to 0.8776,
    "JPY" to 158.46,
    "GBP" to 0.7546,
    "HKD" to 7.8495,
    "TWD" to 31.808,
    "KRW" to 1365.29,
    "AUD" to 1.4229,
    "CAD" to 1.4109,
    "CHF" to 0.8269,
    "SGD" to 1.2795,
    "NZD" to 1.7630,
    "THB" to 33.426,
    "INR" to 95.87,
    "RUB" to 84.85,
    "BRL" to 5.1753,
    "MXN" to 17.6089,
    "XAU" to 0.00023,
    "XAG" to 0.01555,
    "XPT" to 0.00057,
    "XPD" to 0.00079
)
