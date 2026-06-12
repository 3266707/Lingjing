package com.lingjing.feature.perception.engine

import com.lingjing.domain.model.DailyState
import com.lingjing.domain.model.StrategySuggestion
import com.lingjing.domain.model.SuggestionType
import com.lingjing.domain.model.UserMode
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 策略建议引擎
 * 根据用户模式和状态生成具体的修炼建议
 */
@Singleton
class StrategySuggestionEngine @Inject constructor() {

    /**
     * 生成策略建议
     */
    fun generateSuggestions(
        mode: UserMode,
        dailyState: DailyState,
        recentStates: List<DailyState>
    ): List<StrategySuggestion> {
        val suggestions = mutableListOf<StrategySuggestion>()

        when (mode) {
            UserMode.SUPER_GOD -> {
                suggestions.add(
                    StrategySuggestion(
                        type = SuggestionType.DIFFICULTY,
                        title = "突破自我",
                        description = "你已连续${dailyState.consecutiveSuccessDays}天保持高完成率。尝试挑战一个难度5的任务，完成可获得双倍经验！",
                        priority = 3
                    )
                )
            }
            UserMode.STRUGGLING -> {
                suggestions.add(
                    StrategySuggestion(
                        type = SuggestionType.DIFFICULTY,
                        title = "降低难度",
                        description = "建议将明日任务难度统一降低1级，总时长减少20%。稳扎稳打才是长久之道。",
                        priority = 2
                    )
                )
                suggestions.add(
                    StrategySuggestion(
                        type = SuggestionType.REST,
                        title = "增加休息任务",
                        description = "你的真元使用较快，已自动在明日计划中添加一个15分钟的休息任务。",
                        priority = 1
                    )
                )
            }
            UserMode.CRASHED -> {
                suggestions.add(
                    StrategySuggestion(
                        type = SuggestionType.REST,
                        title = "暂停修炼",
                        description = "暂停新任务生成1天。今天只建议完成一个10分钟的冥想休息任务来恢复能量。",
                        priority = 0
                    )
                )
            }
            UserMode.STABLE -> {
                // 检查是否需要微调
                val avgCompletion = recentStates.take(7).map { it.completionRate }.average()
                if (avgCompletion < 0.7f) {
                    suggestions.add(
                        StrategySuggestion(
                            type = SuggestionType.FOCUS,
                            title = "聚焦核心",
                            description = "完成率略低，建议优先完成高优先级任务，低优先级可推迟到明天。",
                            priority = 1
                        )
                    )
                }
            }
        }

        // 通用建议：真元管理
        if (dailyState.zhenyuanUsed > 0) {
            val estimatedMax = maxOf(100, dailyState.zhenyuanRecovered)
            val usageRate = dailyState.zhenyuanUsed.toFloat() / estimatedMax
            if (usageRate > 0.8f) {
                suggestions.add(
                    StrategySuggestion(
                        type = SuggestionType.TIME_MGMT,
                        title = "真元管理",
                        description = "你今日真元消耗超过80%，注意穿插休息任务来恢复真元。",
                        priority = 1
                    )
                )
            }
        }

        // 通用建议：时间管理
        if (dailyState.totalTasks > 6 && dailyState.completionRate < 0.6f) {
            suggestions.add(
                StrategySuggestion(
                    type = SuggestionType.TIME_MGMT,
                    title = "任务精简化",
                    description = "今日任务较多但完成率不高，建议将明日计划控制在5个任务以内。",
                    priority = 2
                )
            )
        }

        return suggestions.sortedBy { it.priority }
    }
}
