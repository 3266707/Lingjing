package com.lingjing.data.repository.impl

import com.lingjing.core.database.dao.DailyStateDao
import com.lingjing.core.database.dao.ReviewLogDao
import com.lingjing.data.local.entity.DailyStateEntity
import com.lingjing.data.local.entity.ReviewLogEntity
import com.lingjing.domain.model.DailyState
import com.lingjing.domain.model.ReviewLog
import com.lingjing.domain.repository.DailyStateRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每日状态仓库实现
 */
@Singleton
class DailyStateRepositoryImpl @Inject constructor(
    private val dailyStateDao: DailyStateDao,
    private val reviewLogDao: ReviewLogDao
) : DailyStateRepository {

    override suspend fun getDailyState(date: String): DailyState? {
        return dailyStateDao.getDailyState(date)?.toDomain()
    }

    override suspend fun getOrCreateDailyState(date: String): DailyState {
        val existing = dailyStateDao.getDailyState(date)
        if (existing != null) return existing.toDomain()

        val newState = DailyStateEntity(
            date = date,
            qiConcentration = 10
        )
        dailyStateDao.insertDailyState(newState)
        return newState.toDomain()
    }

    override suspend fun updateDailyState(state: DailyState) {
        dailyStateDao.updateDailyState(state.toEntity())
    }

    override suspend fun getRecentDailyStates(limit: Int): List<DailyState> {
        return dailyStateDao.getRecentDailyStates(limit).map { it.toDomain() }
    }

    override suspend fun getLatestDailyState(): DailyState? {
        return dailyStateDao.getLatestDailyState()?.toDomain()
    }

    override suspend fun calculateAndUpdateQiConcentration(date: String): Int {
        val recentStates = dailyStateDao.getRecentDailyStates(7)
        if (recentStates.isEmpty()) {
            dailyStateDao.updateQiConcentration(date, 10)
            return 10
        }

        // 灵气浓度计算公式：基于近期完成率和连续成功天数
        val avgCompletion = recentStates.map { it.completionRate }.average()
        val consecutiveSuccess = recentStates.firstOrNull()?.consecutiveSuccessDays ?: 0
        val consecutiveFail = recentStates.firstOrNull()?.consecutiveFailDays ?: 0

        // 低谷期自动提高灵气浓度（补偿机制）
        var qi = when {
            avgCompletion < 0.3 -> 30
            avgCompletion < 0.5 -> 20
            avgCompletion < 0.7 -> 15
            avgCompletion >= 0.9 -> 5
            else -> 10
        }

        // 连续成功降低灵气浓度（不再需要额外助力）
        if (consecutiveSuccess >= 7) qi = maxOf(qi - 5, 0)
        // 连续失败提高灵气浓度
        if (consecutiveFail >= 3) qi = minOf(qi + 10, 50)

        dailyStateDao.updateQiConcentration(date, qi)
        return qi
    }

    override suspend fun insertReviewLog(review: ReviewLog): Long {
        return reviewLogDao.insertReviewLog(review.toEntity())
    }

    override suspend fun updateReviewLog(review: ReviewLog) {
        reviewLogDao.updateReviewLog(review.toEntity())
    }

    override suspend fun getReviewLogByDate(date: String): ReviewLog? {
        return reviewLogDao.getReviewLogByDate(date)?.toDomain()
    }

    override suspend fun getRecentReviews(limit: Int): List<ReviewLog> {
        return try {
            reviewLogDao.getAllReviewLogsOnce().take(limit).map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getAllReviews(): List<ReviewLog> {
        return try {
            reviewLogDao.getAllReviewLogsOnce().map { it.toDomain() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun deleteReviewLog(id: Long) {
        reviewLogDao.deleteReviewLog(id)
    }
}

// Extension mappers
private fun DailyStateEntity.toDomain(): DailyState = DailyState(
    id = id,
    date = date,
    completionRate = completionRate,
    totalTasks = totalTasks,
    completedTasks = completedTasks,
    consecutiveSuccessDays = consecutiveSuccessDays,
    consecutiveFailDays = consecutiveFailDays,
    zhenyuanUsed = zhenyuanUsed,
    zhenyuanRecovered = zhenyuanRecovered,
    sleepQuality = sleepQuality,
    avgDifficultyScore = avgDifficultyScore,
    qiConcentration = qiConcentration,
    emotion = emotion,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun DailyState.toEntity(): DailyStateEntity = DailyStateEntity(
    id = id,
    date = date,
    completionRate = completionRate,
    totalTasks = totalTasks,
    completedTasks = completedTasks,
    consecutiveSuccessDays = consecutiveSuccessDays,
    consecutiveFailDays = consecutiveFailDays,
    zhenyuanUsed = zhenyuanUsed,
    zhenyuanRecovered = zhenyuanRecovered,
    sleepQuality = sleepQuality,
    avgDifficultyScore = avgDifficultyScore,
    qiConcentration = qiConcentration,
    emotion = emotion,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun ReviewLogEntity.toDomain(): ReviewLog = ReviewLog(
    id = id,
    date = date,
    reviewText = reviewText,
    emotion = emotion,
    emotionDetail = emotionDetail,
    unfinishedReason = unfinishedReason,
    expBalanceComment = expBalanceComment,
    attributeBalanceComment = attributeBalanceComment,
    difficultyShift = difficultyShift,
    tomorrowSuggestion = tomorrowSuggestion,
    strategyAdvice = strategyAdvice,
    createdAt = createdAt
)

private fun ReviewLog.toEntity(): ReviewLogEntity = ReviewLogEntity(
    id = id,
    date = date,
    reviewText = reviewText,
    emotion = emotion,
    emotionDetail = emotionDetail,
    unfinishedReason = unfinishedReason,
    expBalanceComment = expBalanceComment,
    attributeBalanceComment = attributeBalanceComment,
    difficultyShift = difficultyShift,
    tomorrowSuggestion = tomorrowSuggestion,
    strategyAdvice = strategyAdvice,
    createdAt = createdAt
)
