package com.lingjing.domain.repository

import com.lingjing.domain.model.DailyState
import com.lingjing.domain.model.ReviewLog

/**
 * 每日状态仓库接口
 */
interface DailyStateRepository {
    suspend fun getDailyState(date: String): DailyState?
    suspend fun getOrCreateDailyState(date: String): DailyState
    suspend fun updateDailyState(state: DailyState)
    suspend fun getRecentDailyStates(limit: Int): List<DailyState>
    suspend fun getLatestDailyState(): DailyState?
    suspend fun calculateAndUpdateQiConcentration(date: String): Int
    suspend fun insertReviewLog(review: ReviewLog): Long
    suspend fun updateReviewLog(review: ReviewLog)
    suspend fun getReviewLogByDate(date: String): ReviewLog?
    suspend fun getRecentReviews(limit: Int): List<ReviewLog>
    suspend fun getAllReviews(): List<ReviewLog>
    suspend fun deleteReviewLog(id: Long)
}
