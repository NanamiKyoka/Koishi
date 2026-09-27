package com.nanami.koishi.core.data.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ToolStorageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ToolStorageDatabase : RoomDatabase() {

    abstract fun toolStorageDao(): ToolStorageDao

    companion object {
        const val DATABASE_NAME = "koishi_tool_storage.db"
    }
}
