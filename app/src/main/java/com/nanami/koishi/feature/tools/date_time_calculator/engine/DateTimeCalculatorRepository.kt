package com.nanami.koishi.feature.tools.date_time_calculator.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao

class DateTimeCalculatorRepository(
    dao: ToolStorageDao
) : BaseToolRepository<DateTimeCalculatorData>(
    toolId = TOOL_ID,
    serializer = DateTimeCalculatorData.serializer(),
    dao = dao,
    defaultData = DateTimeCalculatorData()
) {

    suspend fun save(data: DateTimeCalculatorData): DateTimeCalculatorData = setData(data)

    companion object {
        const val TOOL_ID = "date_time_calculator"
    }
}