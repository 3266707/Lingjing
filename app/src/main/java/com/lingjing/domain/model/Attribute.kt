package com.lingjing.domain.model

/**
 * 属性领域模型
 */
data class Attribute(
    val key: String,
    val name: String,
    val level: Int = 1,
    val currentExp: Long = 0,
    val totalExpEarned: Long = 0,
    val isBase: Boolean = true,
    val weight: Float = 1.0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    /** 升级到下一级所需经验: 等级² × 100 */
    fun expToNextLevel(): Long = (level.toLong() * level * 100)

    /** 经验进度 0.0~1.0 */
    fun getProgress(): Float {
        val required = expToNextLevel()
        return if (required > 0) (currentExp.toFloat() / required).coerceIn(0f, 1f) else 1f
    }

    /** 是否可以升级 */
    fun canLevelUp(): Boolean = currentExp >= expToNextLevel()
}

/**
 * 境界（Realm）
 */
enum class Realm(val minAvgLevel: Int, val displayName: String) {
    QI_REFINING(0, "炼气期"),
    FOUNDATION(10, "筑基期"),
    GOLDEN_CORE(20, "金丹期"),
    NASCENT_SOUL(35, "元婴期"),
    SPIRIT_TRANSFORMATION(50, "化神期");

    companion object {
        fun fromAverageLevel(avgLevel: Float): Realm {
            return entries.lastOrNull { avgLevel >= it.minAvgLevel } ?: QI_REFINING
        }
    }
}

/**
 * 每日状态
 */
data class DailyState(
    val id: Long = 0,
    val date: String = "",
    val completionRate: Float = 0f,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val consecutiveSuccessDays: Int = 0,
    val consecutiveFailDays: Int = 0,
    val zhenyuanUsed: Int = 0,
    val zhenyuanRecovered: Int = 0,
    val sleepQuality: Int? = null,
    val avgDifficultyScore: Float = 0f,
    val qiConcentration: Int = 10,
    val emotion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 用户模式
 */
enum class UserMode(val displayName: String) {
    SUPER_GOD("超神"),
    STABLE("平稳"),
    STRUGGLING("挣扎"),
    CRASHED("宕机");

    companion object {
        fun detect(completionRate: Float, consecutiveSuccess: Int, consecutiveFail: Int): UserMode {
            return when {
                completionRate >= 0.9f && consecutiveSuccess >= 3 -> SUPER_GOD
                completionRate < 0.3f || consecutiveFail >= 3 -> CRASHED
                completionRate in 0.3f..0.6f -> STRUGGLING
                else -> STABLE
            }
        }
    }
}

/**
 * 复盘日志
 */
data class ReviewLog(
    val id: Long = 0,
    val date: String = "",
    val reviewText: String = "",
    val emotion: String? = null,
    val emotionDetail: String? = null,
    val unfinishedReason: String? = null,
    val expBalanceComment: String? = null,
    val attributeBalanceComment: String? = null,
    val difficultyShift: Float = 0f,
    val tomorrowSuggestion: String? = null,
    val strategyAdvice: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 成就
 */
data class Achievement(
    val key: String,
    val name: String,
    val description: String,
    val conditionDescription: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long? = null,
    val rewardType: String? = null,
    val rewardValue: Float = 0f,
    val category: String = "general",
    val displayOrder: Int = 0
)

/**
 * 策略建议
 */
data class StrategySuggestion(
    val type: SuggestionType,
    val title: String,
    val description: String,
    val priority: Int = 0
)

enum class SuggestionType {
    TIME_MGMT, FOCUS, REST, DIFFICULTY, GOAL
}
