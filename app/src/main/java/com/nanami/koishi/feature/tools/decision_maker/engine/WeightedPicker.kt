package com.nanami.koishi.feature.tools.decision_maker.engine

import kotlin.random.Random

/**
 * 加权随机抽取：权重按 1 ~ 10 归一化后落入区间，权重越高被抽中的概率越大
 */
object WeightedPicker {

    fun pick(
        options: List<DecisionOption>,
        random: Random = Random.Default
    ): DecisionOption? {
        val pool = options.filter { it.text.isNotBlank() }
        if (pool.isEmpty()) return null

        val totalWeight = pool.sumOf { DecisionWeight.clamp(it.weight) }
        if (totalWeight <= 0) return pool[random.nextInt(pool.size)]

        var ticket = random.nextInt(totalWeight)
        for (option in pool) {
            ticket -= DecisionWeight.clamp(option.weight)
            if (ticket < 0) return option
        }
        return pool.last()
    }

    /**
     * 选项在转盘上占据的弧度，权重越高扇区越宽
     */
    fun sweepAngles(options: List<DecisionOption>, fullCircle: Float = 360f): List<Float> {
        val pool = options.filter { it.text.isNotBlank() }
        if (pool.isEmpty()) return emptyList()

        val totalWeight = pool.sumOf { DecisionWeight.clamp(it.weight) }.toFloat()
        return pool.map { DecisionWeight.clamp(it.weight) / totalWeight * fullCircle }
    }
}
