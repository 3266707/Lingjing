package com.lingjing.feature.perception.engine

import com.lingjing.core.common.constant.AppConstants
import com.lingjing.domain.model.DailyState
import com.lingjing.domain.model.UserMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户模式检测器
 * 根据近期数据检测用户当前处于哪种模式
 */
@Singleton
class UserModeDetector @Inject constructor() {

    data class DetectionResult(
        val mode: UserMode,
        val completionRate: Float,
        val trend: Trend,  // 趋势
        val consecutiveSuccess: Int,
        val consecutiveFail: Int,
        val message: String,
        val suggestedAction: String
    )

    enum class Trend { RISING, STABLE, DECLINING, UNKNOWN }

    /**
     * 检测用户模式
     */
    fun detect(recentStates: List<DailyState>): DetectionResult {
        if (recentStates.isEmpty()) {
            return DetectionResult(
                mode = UserMode.STABLE,
                completionRate = 0f,
                trend = Trend.UNKNOWN,
                consecutiveSuccess = 0,
                consecutiveFail = 0,
                message = "初入道途，尚未积累足够数据",
                suggestedAction = "完成首次修炼计划以激活系统感知"
            )
        }

        val latest = recentStates.first()
        val avgCompletion = recentStates.take(7).map { it.completionRate }.average().toFloat()
        val trend = calculateTrend(recentStates)

        val mode = when {
            avgCompletion >= AppConstants.SUPER_MODE_THRESHOLD && latest.consecutiveSuccessDays >= 3 -> UserMode.SUPER_GOD
            avgCompletion < AppConstants.CRASH_MODE_THRESHOLD || latest.consecutiveFailDays >= 3 -> UserMode.CRASHED
            avgCompletion in AppConstants.STRUGGLE_MODE_MIN..AppConstants.STRUGGLE_MODE_MAX -> UserMode.STRUGGLING
            else -> UserMode.STABLE
        }

        val (message, action) = getModeMessage(mode, trend, avgCompletion, latest)

        return DetectionResult(
            mode = mode,
            completionRate = avgCompletion,
            trend = trend,
            consecutiveSuccess = latest.consecutiveSuccessDays,
            consecutiveFail = latest.consecutiveFailDays,
            message = message,
            suggestedAction = action
        )
    }

    private fun calculateTrend(states: List<DailyState>): Trend {
        val recent = states.take(3)
        if (recent.size < 2) return Trend.UNKNOWN

        val rates = recent.map { it.completionRate }
        val first = rates.last()  // 最早的
        val last = rates.first()  // 最近的

        val diff = last - first
        return when {
            diff > 0.1f -> Trend.RISING
            diff < -0.1f -> Trend.DECLINING
            else -> Trend.STABLE
        }
    }

    private fun getModeMessage(
        mode: UserMode,
        trend: Trend,
        avgCompletion: Float,
        state: DailyState
    ): Pair<String, String> {
        return when (mode) {
            UserMode.SUPER_GOD -> Pair(
                "道心通明！连续${state.consecutiveSuccessDays}天完成率超90%",
                "可尝试追加挑战任务，开启双倍经验加成"
            )
            UserMode.STABLE -> Pair(
                "修炼平稳，完成率${"%.0f".format(avgCompletion * 100)}%",
                "保持当前节奏，适度微调即可"
            )
            UserMode.STRUGGLING -> when (trend) {
                Trend.DECLINING -> Pair(
                    "近来有些吃力，完成率在持续下滑",
                    "建议降低明日计划总量20%，拆分大任务为小步骤"
                )
                else -> Pair(
                    "修炼略显吃力，完成率${"%.0f".format(avgCompletion * 100)}%",
                    "适当减少任务数量，优先完成核心任务"
                )
            }
            UserMode.CRASHED -> Pair(
                "气海翻涌！近期状态低迷，需要调整",
                "暂停新任务生成，先完成一个简单的休息任务恢复状态。别担心，修行本就是起起伏伏。"
            )
        }
    }

    /**
     * 趋势预警：连续3天完成率下降超过15%
     */
    fun checkTrendWarning(states: List<DailyState>): Boolean {
        if (states.size < 4) return false

        val recent = states.take(AppConstants.TREND_WARNING_DAYS + 1)
        val oldestRate = recent.last().completionRate
        val newestRate = recent.first().completionRate

        val decline = oldestRate - newestRate
        return decline > AppConstants.TREND_DECLINE_THRESHOLD
    }
}
