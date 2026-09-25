package com.nanami.koishi.core.data.storage.sample

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.random.Random

object OptionWeight {
    const val MIN = 1
    const val MAX = 10

    fun clamp(value: Int): Int = value.coerceIn(MIN, MAX)
}

@Serializable
data class WeightedOption(
    val id: String,
    val label: String,
    val weight: Int = OptionWeight.MIN,
    val enabled: Boolean = true
)

@Serializable
data class WeightedOptionSet(
    val options: List<WeightedOption> = emptyList(),
    val lastPickedId: String? = null,
    val recentPickIds: List<String> = emptyList()
) {
    val drawableOptions: List<WeightedOption>
        get() = options.filter { it.enabled && it.label.isNotBlank() && it.weight > 0 }

    companion object {
        const val MAX_RECENT = 20
    }
}

/**
 * 列表型工具示例：无序列表条目整体序列化进单条记录，条目增删与权重调整均走读改写
 */
class WeightedOptionsRepository(
    dao: ToolStorageDao,
    private val random: Random = Random.Default
) : BaseToolRepository<WeightedOptionSet>(
    toolId = TOOL_ID,
    serializer = WeightedOptionSet.serializer(),
    dao = dao,
    defaultData = WeightedOptionSet()
) {

    suspend fun addOption(label: String, weight: Int = OptionWeight.MIN): WeightedOption? {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return null

        val option = WeightedOption(
            id = newOptionId(),
            label = trimmed,
            weight = OptionWeight.clamp(weight)
        )
        val updated = updateData { current ->
            if (current.options.any { it.label == trimmed }) {
                current
            } else {
                current.copy(options = current.options + option)
            }
        }
        return updated.options.firstOrNull { it.id == option.id }
    }

    suspend fun removeOption(optionId: String): WeightedOptionSet =
        updateData { current ->
            current.copy(
                options = current.options.filterNot { it.id == optionId },
                recentPickIds = current.recentPickIds.filterNot { it == optionId },
                lastPickedId = current.lastPickedId.takeIf { it != optionId }
            )
        }

    suspend fun setWeight(optionId: String, weight: Int): WeightedOptionSet =
        updateData { current ->
            current.copy(
                options = current.options.map { option ->
                    if (option.id == optionId) option.copy(weight = OptionWeight.clamp(weight)) else option
                }
            )
        }

    suspend fun setEnabled(optionId: String, enabled: Boolean): WeightedOptionSet =
        updateData { current ->
            current.copy(
                options = current.options.map { option ->
                    if (option.id == optionId) option.copy(enabled = enabled) else option
                }
            )
        }

    suspend fun reorder(orderedIds: List<String>): WeightedOptionSet =
        updateData { current ->
            val byId = current.options.associateBy { it.id }
            val reordered = orderedIds.mapNotNull { byId[it] } +
                current.options.filterNot { it.id in orderedIds }
            current.copy(options = reordered)
        }

    /**
     * 抽取与历史写入合并为一次原子更新，避免并发抽取时历史记录互相覆盖
     */
    suspend fun pick(): WeightedOption? {
        var picked: WeightedOption? = null
        updateData { current ->
            val choice = current.drawableOptions.weightedDraw(random)
            if (choice == null) {
                current
            } else {
                picked = choice
                current.copy(
                    lastPickedId = choice.id,
                    recentPickIds = (listOf(choice.id) + current.recentPickIds.filterNot { it == choice.id })
                        .take(WeightedOptionSet.MAX_RECENT)
                )
            }
        }
        return picked
    }

    private fun newOptionId(): String = "opt_" + UUID.randomUUID().toString().replace("-", "").take(10)

    private fun List<WeightedOption>.weightedDraw(random: Random): WeightedOption? {
        val totalWeight = sumOf { it.weight }
        if (totalWeight <= 0) return null
        var ticket = random.nextInt(totalWeight)
        for (option in this) {
            ticket -= option.weight
            if (ticket < 0) return option
        }
        return lastOrNull()
    }

    companion object {
        const val TOOL_ID = "weighted_options"
    }
}
