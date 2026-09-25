package com.nanami.koishi.feature.tools.currency_converter

import com.nanami.koishi.core.data.repository.InMemoryToolRepository
import com.nanami.koishi.core.model.ToolCategory
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyApi
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConstants
import com.nanami.koishi.feature.tools.currency_converter.engine.CurrencyConverterData
import com.nanami.koishi.feature.tools.currency_converter.engine.defaultFallbackRates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun testToolCategoryAndRegistration() {
        val toolCategory = ToolCategory.CALCULATION
        assertNotNull(toolCategory)

        val repository = InMemoryToolRepository()
        val tool = repository.getToolById("currency_converter")
        assertNotNull(tool)
        assertEquals(ToolCategory.CALCULATION, tool?.category)
    }

    @Test
    fun testPreciousMetalsClassification() {
        assertTrue(CurrencyConstants.isMetal("XAU"))
        assertTrue(CurrencyConstants.isMetal("XAG"))
        assertTrue(CurrencyConstants.isMetal("XPT"))
        assertTrue(CurrencyConstants.isMetal("XPD"))
        assertFalse(CurrencyConstants.isMetal("USD"))
        assertFalse(CurrencyConstants.isMetal("CNY"))
    }

    @Test
    fun testDisplayNameLocalization() {
        val cnyZh = CurrencyConstants.getDisplayName("CNY", "Chinese Yuan", true)
        assertEquals("人民币", cnyZh)

        val cnyEn = CurrencyConstants.getDisplayName("CNY", "Chinese Yuan", false)
        assertEquals("Chinese Yuan", cnyEn)

        val goldZh = CurrencyConstants.getDisplayName("XAU", "Gold (Troy Ounce)", true)
        assertEquals("黄金 (金衡盎司)", goldZh)
    }

    @Test
    fun testMultiCurrencySimultaneousConversion() {
        val rates = mapOf(
            "USD" to 1.0,
            "CNY" to 7.0,
            "EUR" to 0.9,
            "JPY" to 150.0
        )

        val activeCode = "CNY"
        val activeAmount = 700.0
        val activeRate = rates[activeCode] ?: 1.0

        val usdAmount = activeAmount * (rates["USD"]!! / activeRate)
        assertEquals(100.0, usdAmount, 0.001)

        val eurAmount = activeAmount * (rates["EUR"]!! / activeRate)
        assertEquals(90.0, eurAmount, 0.001)

        val jpyAmount = activeAmount * (rates["JPY"]!! / activeRate)
        assertEquals(15000.0, jpyAmount, 0.001)

        val switchedActiveCode = "USD"
        val switchedAmount = 100.0
        val switchedActiveRate = rates[switchedActiveCode] ?: 1.0

        val convertedCny = switchedAmount * (rates["CNY"]!! / switchedActiveRate)
        assertEquals(700.0, convertedCny, 0.001)
    }

    @Test
    fun testFallbackRatesAndCurrencies() {
        val rates = defaultFallbackRates()
        assertTrue(rates.containsKey("USD"))
        assertTrue(rates.containsKey("CNY"))
        assertTrue(rates.containsKey("EUR"))
        assertTrue(rates.containsKey("XAU"))
        assertTrue(rates.containsKey("XAG"))

        val fallbackCurrencies = CurrencyApi.getFallbackCurrencies()
        assertTrue(fallbackCurrencies.any { it.code == "XAU" && it.isPreciousMetal })
        assertTrue(fallbackCurrencies.any { it.code == "USD" && !it.isPreciousMetal })
    }

    @Test
    fun testDefaultConverterData() {
        val data = CurrencyConverterData()
        assertTrue(data.selectedCurrencies.contains("CNY"))
        assertTrue(data.selectedCurrencies.contains("USD"))
        assertTrue(data.selectedCurrencies.contains("XAU"))
        assertTrue(data.selectedCurrencies.size >= 2)
    }
}
