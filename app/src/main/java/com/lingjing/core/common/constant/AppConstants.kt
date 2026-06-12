package com.lingjing.core.common.constant

/**
 * 应用常量
 */
object AppConstants {
    // 数据库
    const val DATABASE_NAME = "lingjing.db"
    const val DATABASE_VERSION = 2

    // DeepSeek API
    const val DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"
    const val DEEPSEEK_TIMEOUT_SECONDS = 10L
    const val DEEPSEEK_MAX_RETRIES = 3
    const val MAX_TOKENS = 4096
    const val TEMPERATURE = 0.3

    // 属性系统
    const val BASE_EXP_FORMULA_MULTIPLIER = 100  // 等级² × 100
    const val DEFAULT_QI_CONCENTRATION = 10      // 默认灵气浓度%
    const val MAX_QI_CONCENTRATION = 50          // 最大灵气浓度%
    const val DEFAULT_DAILY_ENERGY = 100          // 每日初始真元
    const val ENERGY_PER_PHYSIQUE_LEVEL = 5      // 每级体魄增加真元
    const val ENERGY_DEPLETION_THRESHOLD = 0.2   // 真元枯竭阈值
    const val ENERGY_COST_PER_10MIN = 1          // 每10分钟消耗真元
    const val ENERGY_RECOVER_PER_10MIN_REST = 1  // 每10分钟休息恢复

    // 经验计算
    const val BASE_EXP_MIN = 10
    const val BASE_EXP_MAX = 50
    const val DIFFICULTY_MULTIPLIER_BASE = 1.0
    const val DIFFICULTY_MULTIPLIER_STEP = 0.3
    const val QUALITY_MULTIPLIER_MIN = 0.8
    const val QUALITY_MULTIPLIER_MAX = 1.5

    // 用户模式阈值
    const val SUPER_MODE_THRESHOLD = 0.9        // 超神模式：完成率>90%
    const val STABLE_MODE_MIN = 0.6              // 平稳模式：完成率60-80%
    const val STABLE_MODE_MAX = 0.8
    const val STRUGGLE_MODE_MIN = 0.3            // 挣扎模式：完成率30-60%
    const val STRUGGLE_MODE_MAX = 0.6
    const val CRASH_MODE_THRESHOLD = 0.3         // 宕机模式：完成率<30%

    // 趋势预警
    const val TREND_WARNING_DAYS = 3             // 连续N天下降超过阈值触发预警
    const val TREND_DECLINE_THRESHOLD = 0.15     // 下降超过15%

    // 成就
    const val ACHIEVEMENT_STREAK_7_DAYS = 7      // 连续7天完成率≥80%

    // RAG
    const val RAG_SIMILARITY_THRESHOLD = 0.4     // 余弦相似度阈值
    const val RAG_TOP_K = 5                       // 检索Top-K

    // 缓存
    const val PLAN_CACHE_DURATION_HOURS = 24      // 相似计划缓存时间

    // 工作管理
    const val PERCEPTION_CHECK_INTERVAL_HOURS = 4 // 用户模式检测间隔
    const val ENERGY_RECOVERY_INTERVAL_MINUTES = 60L
    const val WIDGET_REFRESH_INTERVAL_MINUTES = 30L

    // 通知ID范围
    const val NOTIFICATION_ID_REMINDER_START = 1000
    const val NOTIFICATION_ID_ENCOURAGE_START = 2000
    const val NOTIFICATION_ID_ACHIEVEMENT_START = 3000
}
