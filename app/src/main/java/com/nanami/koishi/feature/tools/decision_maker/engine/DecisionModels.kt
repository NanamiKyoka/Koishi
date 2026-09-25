package com.nanami.koishi.feature.tools.decision_maker.engine

import kotlinx.serialization.Serializable
import java.util.UUID

object DecisionWeight {
    const val MIN = 1
    const val MAX = 10

    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)
}

/**
 * 单个候选选项，权重取值范围 1 ~ 10，权重越高被抽中概率越大
 */
@Serializable
data class DecisionOption(
    val id: String,
    val text: String,
    val weight: Int = DecisionWeight.MIN
)

@Serializable
data class DecisionTopic(
    val id: String,
    val title: String,
    val options: List<DecisionOption> = emptyList()
) {
    val isBuiltIn: Boolean
        get() = BuiltInPresets.ids.contains(id)
}

enum class DecisionMode {
    WHEEL,
    FORTUNE_STICK
}

enum class DecisionPhase {
    IDLE,
    SHAKING,
    SPINNING,
    DRAWING,
    RESULT
}

object DecisionIds {
    fun newOptionId(): String = "opt_" + UUID.randomUUID().toString().replace("-", "").take(10)

    fun newTopicId(): String = "topic_" + UUID.randomUUID().toString().replace("-", "").take(10)
}

/**
 * 清洗用户输入：去除空白选项与重名选项，并把权重收敛到合法区间
 */
fun DecisionTopic.sanitized(): DecisionTopic {
    val seen = HashSet<String>()
    val cleaned = options.mapNotNull { option ->
        val text = option.text.trim()
        if (text.isEmpty() || !seen.add(text)) {
            null
        } else {
            option.copy(text = text, weight = DecisionWeight.clamp(option.weight))
        }
    }
    return copy(title = title.trim(), options = cleaned)
}

/**
 * 可直接参与抽取的有效选项
 */
val DecisionTopic.playableOptions: List<DecisionOption>
    get() = options.filter { it.text.isNotBlank() }
