package com.nanami.koishi.feature.tools.unit_converter.engine

import kotlinx.serialization.Serializable

@Serializable
data class UnitConverterData(
    val categoryId: String = UnitCategory.LENGTH.id,
    val fromUnitId: String = "meter",
    val toUnitId: String = "kilometer",
    val input: String = "1"
)