package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.*

@Dao
interface SystemDao {

    // UserConfig
    @Query("SELECT * FROM ${DbConstants.TABLE_USER_CONFIG} WHERE config_key = :key")
    suspend fun getConfig(key: String): UserConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setConfig(config: UserConfigEntity)

    @Query("DELETE FROM ${DbConstants.TABLE_USER_CONFIG} WHERE config_key = :key")
    suspend fun deleteConfig(key: String)

    // Offline requests
    @Query("SELECT * FROM ${DbConstants.TABLE_OFFLINE_REQUESTS} WHERE status = 0 ORDER BY created_at ASC")
    suspend fun getPendingOfflineRequests(): List<OfflineRequestEntity>

    @Insert
    suspend fun insertOfflineRequest(request: OfflineRequestEntity): Long

    @Query("UPDATE ${DbConstants.TABLE_OFFLINE_REQUESTS} SET status = :status, retry_count = retry_count + 1 WHERE id = :id")
    suspend fun updateOfflineRequestStatus(id: Long, status: Int)

    @Query("DELETE FROM ${DbConstants.TABLE_OFFLINE_REQUESTS} WHERE status = 2")
    suspend fun deleteCompletedOfflineRequests()

    // Rest task instances
    @Query("SELECT * FROM ${DbConstants.TABLE_REST_TASK_INSTANCES} WHERE status = 1")
    suspend fun getActiveRestTasks(): List<RestTaskInstanceEntity>

    @Insert
    suspend fun insertRestTaskInstance(instance: RestTaskInstanceEntity): Long

    @Update
    suspend fun updateRestTaskInstance(instance: RestTaskInstanceEntity)

    // Energy transactions
    @Query("SELECT * FROM ${DbConstants.TABLE_ENERGY_TRANSACTIONS} ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentEnergyTransactions(limit: Int = 50): List<EnergyTransactionEntity>

    @Query("SELECT SUM(delta) FROM ${DbConstants.TABLE_ENERGY_TRANSACTIONS} WHERE created_at > :sinceTime")
    suspend fun getTotalEnergyDeltaSince(sinceTime: Long): Int?

    @Insert
    suspend fun insertEnergyTransaction(transaction: EnergyTransactionEntity): Long

    // Memory entries
    @Query("SELECT * FROM ${DbConstants.TABLE_MEMORY_ENTRIES} WHERE source_type = :sourceType AND source_id = :sourceId")
    suspend fun getMemoryEntry(sourceType: String, sourceId: Long): MemoryEntryEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_MEMORY_ENTRIES} ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentMemoryEntries(limit: Int = 100): List<MemoryEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemoryEntry(entry: MemoryEntryEntity): Long

    // Task embeddings
    @Query("SELECT * FROM ${DbConstants.TABLE_TASK_EMBEDDINGS} WHERE task_id = :taskId")
    suspend fun getTaskEmbedding(taskId: Long): TaskEmbeddingEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_TASK_EMBEDDINGS} ORDER BY created_at DESC LIMIT :limit")
    suspend fun getAllTaskEmbeddings(limit: Int = 500): List<TaskEmbeddingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskEmbedding(embedding: TaskEmbeddingEntity): Long

    @Query("DELETE FROM ${DbConstants.TABLE_TASK_EMBEDDINGS} WHERE task_id = :taskId")
    suspend fun deleteTaskEmbedding(taskId: Long)
}
