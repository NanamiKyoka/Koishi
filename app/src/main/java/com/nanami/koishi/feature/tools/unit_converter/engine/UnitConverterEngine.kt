package com.nanami.koishi.feature.tools.unit_converter.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

object UnitConverterEngine {

    private val mathContext = MathContext.DECIMAL128
    private val displayContext = MathContext(12, RoundingMode.HALF_UP)

    fun parseInput(input: String): BigDecimal? =
        input.trim().replace(",", "").takeIf { it.isNotBlank() }?.toBigDecimalOrNull()

    fun convert(
        value: BigDecimal,
        category: UnitCategory,
        fromUnitId: String,
        toUnitId: String
    ): BigDecimal? {
        val from = UnitConverterCatalog.find(category, fromUnitId)
        val to = UnitConverterCatalog.find(category, toUnitId)
        val base = value.multiply(from.scale, mathContext).add(from.offset, mathContext)
        return base.subtract(to.offset, mathContext).divide(to.scale, mathContext)
    }

    fun format(value: BigDecimal): String {
        if (value.compareTo(BigDecimal.ZERO) == 0) return "0"
        val rounded = value.round(displayContext).stripTrailingZeros()
        val absolute = rounded.abs()
        return if (absolute < BigDecimal("0.0000001") || absolute >= BigDecimal("1000000000000")) {
            rounded.toEngineeringString()
        } else {
            rounded.toPlainString()
        }
    }
}