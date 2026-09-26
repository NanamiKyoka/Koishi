package com.nanami.koishi.core.data.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ToolStorageDao {

    @Query("SELECT * FROM tool_storage WHERE tool_id = :toolId LIMIT 1")
    fun observe(toolId: String): Flow<ToolStorageEntity?>

    @Query("SELECT * FROM tool_storage")
    fun observeAll(): Flow<List<ToolStorageEntity>>

    @Query("SELECT * FROM tool_storage")
    suspend fun getAll(): List<ToolStorageEntity>

    @Query("SELECT * FROM tool_storage WHERE tool_id = :toolId LIMIT 1")
    suspend fun find(toolId: String): ToolStorageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ToolStorageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<ToolStorageEntity>)

    @Query("DELETE FROM tool_storage WHERE tool_id = :toolId")
    suspend fun delete(toolId: String): Int

    @Query("DELETE FROM tool_storage")
    suspend fun deleteAll(): Int
}
