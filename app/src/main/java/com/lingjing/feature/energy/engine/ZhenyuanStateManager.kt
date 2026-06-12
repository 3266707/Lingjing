package com.lingjing.feature.energy.engine

import com.lingjing.core.common.constant.AppConstants
import com.lingjing.data.local.entity.EnergyTransactionEntity
import com.lingjing.core.database.dao.SystemDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 真元状态管理器
 * 管理真元消耗、恢复、枯竭检测
 */
@Singleton
class ZhenyuanStateManager @Inject constructor(
    private val systemDao: SystemDao
) {

    companion object {
        const val CATEGORY_TASK_COMPLETE = "task_complete"
        const val CATEGORY_REST = "rest"
        const val CATEGORY_DAILY_RESET = "daily_reset"
        const val CATEGORY_SLEEP_BONUS = "sleep_bonus"
    }

    /**
     * 计算初始真元上限
     * = 100 + 道体·体魄等级 × 5 + 睡眠质量加成
     */
    fun calculateMaxEnergy(physiqueLevel: Int, sleepQuality: Int?): Int {
        val base = AppConstants.DEFAULT_DAILY_ENERGY
        val physiqueBonus = physiqueLevel * AppConstants.ENERGY_PER_PHYSIQUE_LEVEL
        val sleepBonus = (sleepQuality ?: 3) * 2  // 每级睡眠质量 +2
        return base + physiqueBonus + sleepBonus
    }

    /**
     * 计算任务真元消耗
     * = max(1, 预估分钟数 / 10)
     */
    fun calculateTaskCost(estimatedMinutes: Int): Int {
        return maxOf(1, estimatedMinutes / 10)
    }

    /**
     * 计算休息恢复量
     * = 休息时长(分钟) / 10
     */
    fun calculateRestRecovery(restMinutes: Int): Int {
        return restMinutes / 10
    }

    /**
     * 获取今日真元使用总量
     */
    suspend fun getTodayEnergyUsed(todayStart: Long): Int {
        val transactions = systemDao.getRecentEnergyTransactions(100)
        return transactions
            .filter { it.delta < 0 && it.createdAt >= todayStart }
            .sumOf { -it.delta }
    }

    /**
     * 检查真元是否枯竭
     * 剩余 < 20% 视为枯竭
     */
    fun isEnergyDepleted(currentEnergy: Int, maxEnergy: Int): Boolean {
        if (maxEnergy <= 0) return true
        return currentEnergy.toFloat() / maxEnergy <= AppConstants.ENERGY_DEPLETION_THRESHOLD
    }

    /**
     * 消耗真元
     */
    suspend fun consumeEnergy(amount: Int, taskId: Long, currentBalance: Int): Int {
        val newBalance = currentBalance - amount
        systemDao.insertEnergyTransaction(
            EnergyTransactionEntity(
                delta = -amount,
                reasonCategory = CATEGORY_TASK_COMPLETE,
                reasonId = taskId,
                balanceAfter = newBalance,
                description = "完成任务消耗真元"
            )
        )
        return newBalance
    }

    /**
     * 恢复真元
     */
    suspend fun recoverEnergy(amount: Int, reason: String, currentBalance: Int): Int {
        val newBalance = currentBalance + amount
        systemDao.insertEnergyTransaction(
            EnergyTransactionEntity(
                delta = amount,
                reasonCategory = reason,
                balanceAfter = newBalance,
                description = if (reason == CATEGORY_REST) "休息恢复真元" else "真元恢复"
            )
        )
        return newBalance
    }

    /**
     * 检查真元枯竭后的经验衰减
     * 枯竭时经验减半
     */
    fun getEnergyExpMultiplier(currentEnergy: Int, maxEnergy: Int): Float {
        return if (isEnergyDepleted(currentEnergy, maxEnergy)) 0.5f else 1.0f
    }
}
