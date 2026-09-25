package com.nanami.koishi.feature.tools.decision_maker.engine

import android.content.Context
import androidx.annotation.StringRes
import com.nanami.koishi.R

/**
 * 内置经典预设主题，内容取自字符串资源以跟随应用语言，用户编辑后仅保存覆盖副本
 */
object BuiltInPresets {

    const val MEAL_ID = "builtin_meal"

    val ids: Set<String> = setOf(MEAL_ID)

    val defaultTopicId: String = MEAL_ID

    fun build(context: Context): List<DecisionTopic> = listOf(
        topic(
            context = context,
            id = MEAL_ID,
            titleRes = R.string.preset_meal_title,
            optionRes = listOf(
                R.string.preset_meal_1,
                R.string.preset_meal_2,
                R.string.preset_meal_3,
                R.string.preset_meal_4,
                R.string.preset_meal_5,
                R.string.preset_meal_6,
                R.string.preset_meal_7,
                R.string.preset_meal_8
            )
        )
    )

    private fun topic(
        context: Context,
        id: String,
        @StringRes titleRes: Int,
        @StringRes optionRes: List<Int>
    ): DecisionTopic = DecisionTopic(
        id = id,
        title = context.getString(titleRes),
        options = optionRes.mapIndexed { index, res ->
            DecisionOption(id = "$id:$index", text = context.getString(res))
        }
    )
}
