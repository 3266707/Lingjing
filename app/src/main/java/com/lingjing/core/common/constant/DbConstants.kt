package com.lingjing.core.common.constant

/**
 * 数据库相关常量
 */
object DbConstants {
    // 表名
    const val TABLE_TASKS = "tasks"
    const val TABLE_ATTRIBUTES = "attributes"
    const val TABLE_GOALS = "goals"
    const val TABLE_REVIEW_LOGS = "review_logs"
    const val TABLE_DAILY_STATE = "daily_state"
    const val TABLE_ACHIEVEMENTS = "achievements"
    const val TABLE_USER_CONFIG = "user_config"
    const val TABLE_TASK_EMBEDDINGS = "task_embeddings"
    const val TABLE_OFFLINE_REQUESTS = "offline_requests"
    const val TABLE_PLANS = "plans"
    const val TABLE_PLAN_TASKS = "plan_tasks"
    const val TABLE_REPEAT_TASK_CONFIGS = "repeat_task_configs"
    const val TABLE_PLAN_TEMPLATES = "plan_templates"
    const val TABLE_LONG_TERM_GOALS = "long_term_goals"
    const val TABLE_REST_TASK_INSTANCES = "rest_task_instances"
    const val TABLE_ENERGY_TRANSACTIONS = "zhenyuan_transactions"
    const val TABLE_MEMORY_ENTRIES = "memory_entries"

    // 属性类型
    const val ATTR_WISDOM = "wisdom"       // 灵根·悟性
    const val ATTR_PHYSIQUE = "physique"   // 道体·体魄
    const val ATTR_PERCEPTION = "perception" // 神识·感知
    const val ATTR_ENERGY = "energy"       // 真元·精力
    const val ATTR_WILL = "will"           // 丹心·意志

    val BASE_ATTRIBUTES = listOf(ATTR_WISDOM, ATTR_PHYSIQUE, ATTR_PERCEPTION, ATTR_ENERGY, ATTR_WILL)

    // 任务状态
    const val TASK_PENDING = 0
    const val TASK_DONE = 1
    const val TASK_SKIPPED = 2
    const val TASK_DEFERRED = 3

    // 计划类型
    const val PLAN_TYPE_DAILY = "daily"
    const val PLAN_TYPE_LONG_TERM = "long_term"
    const val PLAN_TYPE_REPEAT = "repeat"

    // 情绪
    const val EMOTION_POSITIVE = "positive"
    const val EMOTION_NEUTRAL = "neutral"
    const val EMOTION_NEGATIVE = "negative"

    // AI人设
    const val PERSONA_RATIONAL = "rational"       // 理性引导型
    const val PERSONA_PASSIONATE = "passionate"   // 热血鼓励型
    const val PERSONA_TSUNDERE = "tsundere"       // 毒舌吐槽型
    const val PERSONA_GENTLE = "gentle"           // 温柔治愈型
}
