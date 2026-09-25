package com.nanami.koishi.core.data.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 所有工具共用的单条存储记录，具体业务数据以 JSON 形式存放于 [payloadJson]
 */
@Entity(tableName = "tool_storage")
data class ToolStorageEntity(
    @PrimaryKey
    @ColumnInfo(name = "tool_id")
    val toolId: String,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
