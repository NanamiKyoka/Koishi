package com.nanami.koishi.feature.tools.bmi_calculator.engine

import com.nanami.koishi.core.data.storage.BaseToolRepository
import com.nanami.koishi.core.data.storage.ToolStorageDao
import java.util.UUID

class BmiRepository(
    dao: ToolStorageDao
) : BaseToolRepository<BmiCalculatorData>(
    toolId = TOOL_ID,
    serializer = BmiCalculatorData.serializer(),
    dao = dao,
    defaultData = BmiCalculatorData()
) {

    suspend fun addRecord(
        heightCm: Double,
        weightKg: Double,
        timestamp: Long = System.currentTimeMillis()
    ): BmiCalculatorData = updateData { current ->
        current.copy(
            records = current.records + BmiRecord(
                id = UUID.randomUUID().toString(),
                timestamp = timestamp,
                heightCm = heightCm,
                weightKg = weightKg
            ),
            draftHeightCm = BmiCalculator.formatValue(heightCm),
            draftWeightKg = BmiCalculator.formatValue(weightKg)
        )
    }

    suspend fun restoreRecord(record: BmiRecord): BmiCalculatorData = updateData { current ->
        if (current.records.any { it.id == record.id }) {
            current
        } else {
            current.copy(records = (current.records + record).sortedBy { it.timestamp })
        }
    }

    suspend fun removeRecord(recordId: String): BmiCalculatorData = updateData { current ->
        current.copy(records = current.records.filterNot { it.id == recordId })
    }

    suspend fun clearRecords(): BmiCalculatorData = updateData { current ->
        current.copy(records = emptyList())
    }

    suspend fun updateDraft(heightText: String, weightText: String): BmiCalculatorData =
        updateData { current -> current.copy(draftHeightCm = heightText, draftWeightKg = weightText) }

    companion object {
        const val TOOL_ID = "bmi_calculator"
    }
}
