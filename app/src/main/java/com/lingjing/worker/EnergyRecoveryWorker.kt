package com.lingjing.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.domain.repository.AttributeRepository
import com.lingjing.domain.repository.DailyStateRepository
import com.lingjing.feature.energy.engine.ZhenyuanStateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * 真元被动恢复 Worker
 * 每小时执行一次，基于经过的时间计算被动真元恢复并向 ZhenyuanStateManager 登记。
 * 恢复量 = base_rate * hours_passed，不发送通知。
 */
@HiltWorker
class EnergyRecoveryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dailyStateRepository: DailyStateRepository,
    private val zhenyuanStateManager: ZhenyuanStateManager,
    private val attributeRepository: AttributeRepository
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "EnergyRecoveryWorker"

        /**
         * 被动真元恢复基础速率（每小时恢复量）
         * 主动休息恢复为 6/小时（每10分钟恢复1），被动恢复取其约 1/3。
         */
        private const val PASSIVE_RECOVERY_RATE_PER_HOUR = 2
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "开始计算真元被动恢复")

            // 1. 获取今日日期 & 状态
            val today = DateTimeUtils.today()
            val todayState = dailyStateRepository.getOrCreateDailyState(today)

            // 2. 获取体魄等级用于计算真元上限
            val baseAttributes = attributeRepository.getBaseAttributes().first()
            val physiqueLevel = baseAttributes
                .find { it.key == "physique" }
                ?.level
                ?: 1

            // 3. 计算真元上限（MaxEnergy = 100 + 体魄等级 x 5 + 睡眠加成）
            val maxEnergy = zhenyuanStateManager.calculateMaxEnergy(
                physiqueLevel = physiqueLevel,
                sleepQuality = todayState.sleepQuality
            )

            // 4. 计算当前剩余真元（近似值 = 上限 - 今日消耗 + 今日恢复）
            val currentBalance = (maxEnergy - todayState.zhenyuanUsed + todayState.zhenyuanRecovered)
                .coerceIn(0, maxEnergy)

            // 5. 计算被动恢复量 base_rate * hours_passed
            //    该 Worker 按 ENERY_RECOVERY_INTERVAL_MINUTES (60) 调度，
            //    假设每次运行时已过去 ~1 小时；若需要精确追踪可在后续版本引入
            //    持久化 lastRunTimestamp 的方案。
            val hoursPassed = 1
            val recoveryAmount = PASSIVE_RECOVERY_RATE_PER_HOUR * hoursPassed

            Log.d(TAG, "被动恢复: baseRate=$PASSIVE_RECOVERY_RATE_PER_HOUR, " +
                    "hours=$hoursPassed, amount=$recoveryAmount, " +
                    "currentBalance=$currentBalance, maxEnergy=$maxEnergy")

            // 6. 通过 ZhenyuanStateManager 登记恢复
            if (recoveryAmount > 0 && currentBalance < maxEnergy) {
                val cappedAmount = minOf(recoveryAmount, maxEnergy - currentBalance)
                zhenyuanStateManager.recoverEnergy(
                    amount = cappedAmount,
                    reason = ZhenyuanStateManager.CATEGORY_REST,
                    currentBalance = currentBalance
                )
                Log.d(TAG, "恢复完成: +$cappedAmount 真元")
            } else {
                Log.d(TAG, "真元已满或恢复量为0，跳过本次恢复")
            }

            // 不发送通知（被动恢复静默执行）
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "真元被动恢复失败", e)
            Result.retry()
        }
    }
}
