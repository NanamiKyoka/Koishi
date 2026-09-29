package com.nanami.koishi.feature.tools.worth_calculator.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import java.util.UUID

class WorthRepository(
    dao: ToolStorageDao
) : BaseToolRepository<WorthCalculatorData>(
    toolId = TOOL_ID,
    serializer = WorthCalculatorData.serializer(),
    dao = dao,
    defaultData = WorthCalculatorData()
) {

    suspend fun saveRecord(
        record: WorthRecord
    ): WorthCalculatorData = updateData { current ->
        current.copy(
            records = (listOf(record) + current.records).take(50)
        )
    }

    suspend fun deleteRecord(recordId: String): WorthCalculatorData = updateData { current ->
        current.copy(records = current.records.filterNot { it.id == recordId })
    }

    suspend fun clearRecords(): WorthCalculatorData = updateData { current ->
        current.copy(records = emptyList())
    }

    suspend fun saveDraft(data: WorthCalculatorData): WorthCalculatorData = updateData { current ->
        data.copy(records = current.records)
    }

    companion object {
        const val TOOL_ID = "worth_calculator"
    }
}
