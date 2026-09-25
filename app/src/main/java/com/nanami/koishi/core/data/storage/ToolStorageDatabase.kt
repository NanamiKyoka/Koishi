package com.nanami.koishi.core.data.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ToolStorageEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ToolStorageDatabase : RoomDatabase() {

    abstract fun toolStorageDao(): ToolStorageDao

    companion object {

        private const val DATABASE_NAME = "koishi_tool_storage.db"

        @Volatile
        private var instance: ToolStorageDatabase? = null

        fun get(context: Context): ToolStorageDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    ToolStorageDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { instance = it }
            }
    }
}
