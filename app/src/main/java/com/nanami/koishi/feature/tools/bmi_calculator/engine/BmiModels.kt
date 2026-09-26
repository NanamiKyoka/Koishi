package com.nanami.koishi.feature.tools.bmi_calculator.engine

import androidx.annotation.StringRes
import com.nanami.koishi.R
import kotlinx.serialization.Serializable

/**
 * 中国成人 BMI 分级标准，区间左闭右开
 */
enum class BmiCategory(@StringRes val labelRes: Int) {
    UNDERWEIGHT(R.string.bmi_category_underweight),
    NORMAL(R.string.bmi_category_normal),
    OVERWEIGHT(R.string.bmi_category_overweight),
    OBESE(R.string.bmi_category_obese)
}

@Serializable
data class BmiRecord(
    val id: String,
    val timestamp: Long,
    val heightCm: Double,
    val weightKg: Double
) {
    val bmi: Double
        get() = BmiCalculator.calculateBmi(heightCm, weightKg)

    val category: BmiCategory
        get() = BmiCalculator.categoryOf(bmi)
}

@Serializable
data class BmiCalculatorData(
    val records: List<BmiRecord> = emptyList(),
    val draftHeightCm: String = "",
    val draftWeightKg: String = ""
)
