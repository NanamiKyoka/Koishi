package com.nanami.koishi.feature.tools.currency_converter.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

object CurrencyApi {

    private const val BASE_URL = "https://api.frankfurter.dev/v2"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    @Serializable
    private data class RawCurrencyItem(
        val iso_code: String,
        val name: String,
        val symbol: String? = null
    )

    @Serializable
    private data class RawRateItem(
        val date: String,
        val base: String,
        val quote: String,
        val rate: Double
    )

    suspend fun fetchCurrencies(): List<CurrencyInfo> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/currencies")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to fetch currencies: HTTP ${response.code}")
        }

        val body = response.body?.string().orEmpty()
        val rawList = json.decodeFromString<List<RawCurrencyItem>>(body)

        val parsed = rawList.map { item ->
            CurrencyInfo(
                code = item.iso_code,
                name = item.name,
                symbol = CurrencyConstants.resolveSymbol(item.iso_code, item.symbol),
                isPreciousMetal = CurrencyConstants.isMetal(item.iso_code)
            )
        }.toMutableList()

        ensurePreciousMetals(parsed)
        parsed.sortedWith(compareBy({ !it.isPreciousMetal }, { it.code }))
    }

    suspend fun fetchRates(base: String = "USD"): Pair<String, Map<String, Double>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$BASE_URL/rates?base=$base")
            .header("Accept", "application/json")
            .build()

        val response = httpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            throw IOException("Failed to fetch rates: HTTP ${response.code}")
        }

        val body = response.body?.string().orEmpty()
        val rawList = json.decodeFromString<List<RawRateItem>>(body)

        val rateMap = mutableMapOf<String, Double>()
        rateMap[base] = 1.0
        var latestDate = ""

        for (item in rawList) {
            rateMap[item.quote] = item.rate
            if (latestDate.isEmpty()) {
                latestDate = item.date
            }
        }

        Pair(latestDate.ifEmpty { "Latest" }, rateMap)
    }

    fun getFallbackCurrencies(): List<CurrencyInfo> {
        val defaultList = mutableListOf(
            CurrencyInfo("CNY", "Chinese Renminbi Yuan", "¥", false),
            CurrencyInfo("USD", "United States Dollar", "$", false),
            CurrencyInfo("EUR", "Euro", "€", false),
            CurrencyInfo("JPY", "Japanese Yen", "¥", false),
            CurrencyInfo("GBP", "British Pound", "£", false),
            CurrencyInfo("HKD", "Hong Kong Dollar", "$", false),
            CurrencyInfo("TWD", "New Taiwan Dollar", "$", false),
            CurrencyInfo("KRW", "South Korean Won", "₩", false),
            CurrencyInfo("AUD", "Australian Dollar", "$", false),
            CurrencyInfo("CAD", "Canadian Dollar", "$", false),
            CurrencyInfo("CHF", "Swiss Franc", "CHF", false),
            CurrencyInfo("SGD", "Singapore Dollar", "$", false),
            CurrencyInfo("NZD", "New Zealand Dollar", "$", false),
            CurrencyInfo("THB", "Thai Baht", "฿", false),
            CurrencyInfo("INR", "Indian Rupee", "₹", false),
            CurrencyInfo("RUB", "Russian Ruble", "₽", false),
            CurrencyInfo("BRL", "Brazilian Real", "R$", false),
            CurrencyInfo("MXN", "Mexican Peso", "$", false),
            CurrencyInfo("ZAR", "South African Rand", "R", false),
            CurrencyInfo("TRY", "Turkish Lira", "₺", false),
            CurrencyInfo("AED", "United Arab Emirates Dirham", "د.إ", false),
            CurrencyInfo("SAR", "Saudi Riyal", "ر.س", false)
        )
        ensurePreciousMetals(defaultList)
        return defaultList.sortedWith(compareBy({ !it.isPreciousMetal }, { it.code }))
    }

    private fun ensurePreciousMetals(list: MutableList<CurrencyInfo>) {
        val metals = listOf(
            CurrencyInfo("XAU", "Gold (Troy Ounce)", "oz t", true),
            CurrencyInfo("XAG", "Silver (Troy Ounce)", "oz t", true),
            CurrencyInfo("XPT", "Platinum", "oz t", true),
            CurrencyInfo("XPD", "Palladium", "oz t", true)
        )
        metals.forEach { metal ->
            if (list.none { it.code.equals(metal.code, ignoreCase = true) }) {
                list.add(metal)
            }
        }
    }
}
