package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.ReviewLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewLogDao {

    @Query("SELECT * FROM ${DbConstants.TABLE_REVIEW_LOGS} ORDER BY date DESC")
    fun getAllReviewLogs(): Flow<List<ReviewLogEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_REVIEW_LOGS} WHERE date = :date")
    suspend fun getReviewLogByDate(date: String): ReviewLogEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_REVIEW_LOGS} WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getReviewLogsInRange(startDate: String, endDate: String): List<ReviewLogEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_REVIEW_LOGS} WHERE emotion = :emotion ORDER BY date DESC LIMIT :limit")
    suspend fun getReviewsByEmotion(emotion: String, limit: Int = 10): List<ReviewLogEntity>

    @Insert
    suspend fun insertReviewLog(log: ReviewLogEntity): Long

    @Update
    suspend fun updateReviewLog(log: ReviewLogEntity)

    @Query("SELECT * FROM ${DbConstants.TABLE_REVIEW_LOGS} ORDER BY date DESC")
    suspend fun getAllReviewLogsOnce(): List<ReviewLogEntity>

    @Query("DELETE FROM ${DbConstants.TABLE_REVIEW_LOGS} WHERE id = :id")
    suspend fun deleteReviewLog(id: Long)
}
