package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.DailyStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStateDao {

    @Query("SELECT * FROM ${DbConstants.TABLE_DAILY_STATE} WHERE date = :date")
    suspend fun getDailyState(date: String): DailyStateEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_DAILY_STATE} WHERE date = :date")
    fun getDailyStateFlow(date: String): Flow<DailyStateEntity?>

    @Query("SELECT * FROM ${DbConstants.TABLE_DAILY_STATE} WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getDailyStatesInRange(startDate: String, endDate: String): List<DailyStateEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_DAILY_STATE} ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentDailyStates(limit: Int = 30): List<DailyStateEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_DAILY_STATE} ORDER BY date DESC LIMIT 1")
    suspend fun getLatestDailyState(): DailyStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyState(state: DailyStateEntity): Long

    @Update
    suspend fun updateDailyState(state: DailyStateEntity)

    @Query("UPDATE ${DbConstants.TABLE_DAILY_STATE} SET qi_concentration = :qi, updated_at = :updatedAt WHERE date = :date")
    suspend fun updateQiConcentration(date: String, qi: Int, updatedAt: Long = System.currentTimeMillis())
}
