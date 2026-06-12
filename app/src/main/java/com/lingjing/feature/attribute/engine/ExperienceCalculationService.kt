package com.lingjing.feature.attribute.engine

import com.lingjing.core.common.constant.AppConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 经验值计算服务
 *
 * 经验 = 基础分(10~50) × 难度系数(1.0~2.5) × 完成质量系数(0.8~1.5) × (1 + 灵气浓度/100)
 */
@Singleton
class ExperienceCalculationService @Inject constructor() {

    data class ExpInput(
        val baseExp: Int = 20,               // 基础分 (10-50)
        val difficultyScore: Int = 3,         // 难度分 (1-5)
        val qualityScore: Int = 3,            // 完成质量 (1-5, 用户自评)
        val estimatedMinutes: Int = 30,       // 预估耗时
        val actualMinutes: Int? = null,       // 实际耗时
        val qiConcentration: Int = 10         // 灵气浓度 (0-50)
    )

    /**
     * 计算最终经验值
     */
    fun calculate(input: ExpInput): Long {
        val base = input.baseExp.toDouble()
        val difficulty = calculateDifficultyMultiplier(input.difficultyScore)
        val quality = calculateQualityMultiplier(input.qualityScore, input.estimatedMinutes, input.actualMinutes)
        val qi = 1.0 + input.qiConcentration / 100.0

        val result = base * difficulty * quality * qi
        return result.roundToLong().coerceAtLeast(1)
    }

    private fun Double.roundToLong(): Long = Math.round(this)

    /**
     * 难度系数: 1.0 + (难度分 - 1) × 0.3
     * 范围: 1.0 ~ 2.2
     */
    fun calculateDifficultyMultiplier(difficultyScore: Int): Double {
        val score = difficultyScore.coerceIn(1, 5)
        return AppConstants.DIFFICULTY_MULTIPLIER_BASE +
                (score - 1) * AppConstants.DIFFICULTY_MULTIPLIER_STEP
    }

    /**
     * 完成质量系数: 基于用户自评 1-5
     * 1分 → 0.8, 2分 → 0.95, 3分 → 1.1, 4分 → 1.3, 5分 → 1.5
     */
    fun calculateQualityMultiplier(
        qualityScore: Int,
        estimatedMinutes: Int,
        actualMinutes: Int?
    ): Double {
        val score = qualityScore.coerceIn(1, 5)

        // 基础质量系数
        val baseQuality = when (score) {
            1 -> 0.8
            2 -> 0.95
            3 -> 1.1
            4 -> 1.3
            5 -> 1.5
            else -> 1.0
        }

        // 时间效率修正
        if (actualMinutes != null && estimatedMinutes > 0) {
            val efficiency = actualMinutes.toDouble() / estimatedMinutes
            val timeBonus = when {
                efficiency <= 0.5 -> 0.2   // 提前一半完成 +20%
                efficiency <= 0.8 -> 0.1   // 提前完成 +10%
                efficiency <= 1.2 -> 0.0   // 按时完成不变
                efficiency <= 1.5 -> -0.1  // 延后完成 -10%
                else -> -0.2               // 严重超时 -20%
            }
            return (baseQuality + timeBonus).coerceIn(
                AppConstants.QUALITY_MULTIPLIER_MIN,
                AppConstants.QUALITY_MULTIPLIER_MAX
            )
        }

        return baseQuality.coerceIn(
            AppConstants.QUALITY_MULTIPLIER_MIN,
            AppConstants.QUALITY_MULTIPLIER_MAX
        )
    }

    /**
     * 获取难度对应的显示名称
     */
    fun getDifficultyName(score: Int): String = when (score) {
        1 -> "轻松"
        2 -> "简单"
        3 -> "普通"
        4 -> "困难"
        5 -> "地狱"
        else -> "未知"
    }
}
