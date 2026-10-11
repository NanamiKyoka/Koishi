package com.nanami.koishi.feature.tools.unit_converter.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao

class UnitConverterRepository(
    dao: ToolStorageDao
) : BaseToolRepository<UnitConverterData>(
    toolId = TOOL_ID,
    serializer = UnitConverterData.serializer(),
    dao = dao,
    defaultData = UnitConverterData()
) {

    suspend fun updateDraft(
        categoryId: String,
        fromUnitId: String,
        toUnitId: String,
        input: String
    ): UnitConverterData = updateData { current ->
        current.copy(
            categoryId = categoryId,
            fromUnitId = fromUnitId,
            toUnitId = toUnitId,
            input = input
        )
    }

    companion object {
        const val TOOL_ID = "unit_converter"
    }
}