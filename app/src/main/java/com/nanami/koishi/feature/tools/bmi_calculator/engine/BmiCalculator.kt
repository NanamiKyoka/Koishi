package com.nanami.koishi.feature.tools.bmi_calculator.engine

import java.util.Locale
import kotlin.math.pow

object BmiCalculator {

    const val MIN_HEIGHT_CM = 50.0
    const val MAX_HEIGHT_CM = 250.0
    const val MIN_WEIGHT_KG = 10.0
    const val MAX_WEIGHT_KG = 500.0

    const val UNDERWEIGHT_MAX_BMI = 18.5
    const val NORMAL_MAX_BMI = 24.0
    const val OVERWEIGHT_MAX_BMI = 28.0

    private const val OBESE_SCALE_CEILING = 40.0

    fun calculateBmi(heightCm: Double, weightKg: Double): Double {
        if (heightCm <= 0.0 || weightKg <= 0.0) return 0.0
        val heightMeters = heightCm / 100.0
        return weightKg / heightMeters.pow(2)
    }

    fun categoryOf(bmi: Double): BmiCategory = when {
        bmi < UNDERWEIGHT_MAX_BMI -> BmiCategory.UNDERWEIGHT
        bmi < NORMAL_MAX_BMI -> BmiCategory.NORMAL
        bmi < OVERWEIGHT_MAX_BMI -> BmiCategory.OVERWEIGHT
        else -> BmiCategory.OBESE
    }

    fun categoryRangeOf(category: BmiCategory): ClosedFloatingPointRange<Double> = when (category) {
        BmiCategory.UNDERWEIGHT -> 0.0..UNDERWEIGHT_MAX_BMI
        BmiCategory.NORMAL -> UNDERWEIGHT_MAX_BMI..NORMAL_MAX_BMI
        BmiCategory.OVERWEIGHT -> NORMAL_MAX_BMI..OVERWEIGHT_MAX_BMI
        BmiCategory.OBESE -> OVERWEIGHT_MAX_BMI..OBESE_SCALE_CEILING
    }

    fun healthyWeightRange(heightCm: Double): ClosedFloatingPointRange<Double> {
        val squared = (heightCm / 100.0).pow(2)
        return (UNDERWEIGHT_MAX_BMI * squared)..(NORMAL_MAX_BMI * squared)
    }

    /**
     * 体重偏离健康区间的差值，落在区间内返回 0，负数偏轻、正数偏重
     */
    fun healthyWeightDeviation(heightCm: Double, weightKg: Double): Double {
        val range = healthyWeightRange(heightCm)
        return when {
            weightKg < range.start -> weightKg - range.start
            weightKg > range.endInclusive -> weightKg - range.endInclusive
            else -> 0.0
        }
    }

    /**
     * 将 BMI 映射到四段等宽刻度上的相对位置，用于仪表盘指针定位
     */
    fun gaugeFraction(bmi: Double): Float {
        val (segmentIndex, segmentProgress) = when {
            bmi < UNDERWEIGHT_MAX_BMI -> 0 to bmi / UNDERWEIGHT_MAX_BMI
            bmi < NORMAL_MAX_BMI -> 1 to
                    (bmi - UNDERWEIGHT_MAX_BMI) / (NORMAL_MAX_BMI - UNDERWEIGHT_MAX_BMI)
            bmi < OVERWEIGHT_MAX_BMI -> 2 to
                    (bmi - NORMAL_MAX_BMI) / (OVERWEIGHT_MAX_BMI - NORMAL_MAX_BMI)
            else -> 3 to
                    (bmi - OVERWEIGHT_MAX_BMI) / (OBESE_SCALE_CEILING - OVERWEIGHT_MAX_BMI)
        }
        val progress = segmentProgress.coerceIn(0.0, 1.0)
        return ((segmentIndex + progress) / BmiCategory.entries.size).toFloat()
    }

    fun isValidHeight(heightCm: Double): Boolean = heightCm in MIN_HEIGHT_CM..MAX_HEIGHT_CM

    fun isValidWeight(weightKg: Double): Boolean = weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG

    fun formatBmi(bmi: Double): String = String.format(Locale.US, "%.1f", bmi)

    fun formatValue(value: Double): String =
        if (value % 1.0 == 0.0) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
}
