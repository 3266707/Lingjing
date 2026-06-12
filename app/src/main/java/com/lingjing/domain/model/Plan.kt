package com.lingjing.domain.model

/**
 * 计划领域模型
 */
data class Plan(
    val planId: Long = 0,
    val type: PlanType = PlanType.DAILY,
    val title: String = "",
    val sourceText: String = "",
    val date: String = "",
    val isTemplate: Boolean = false,
    val tasks: List<PlanTask> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val completedTasks: Int get() = tasks.count { it.status == TaskStatus.DONE }
    val totalTasks: Int get() = tasks.size
    val completionRate: Float get() = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 0f
    val isComplete: Boolean get() = tasks.all { it.status == TaskStatus.DONE || it.status == TaskStatus.SKIPPED }
}

enum class PlanType(val value: String) {
    DAILY("daily"),
    LONG_TERM("long_term"),
    REPEAT("repeat");

    companion object {
        fun fromValue(value: String): PlanType =
            entries.find { it.value == value } ?: DAILY
    }
}

/**
 * 计划任务领域模型
 */
data class PlanTask(
    val taskId: Long = 0,
    val planId: Long = 0,
    val name: String = "",
    val attributeKey: String = "wisdom",
    val estimatedMinutes: Int = 30,
    val actualMinutes: Int? = null,
    val difficultyScore: Int = 3,
    val priority: Priority = Priority.MEDIUM,
    val orderIndex: Int = 0,
    val status: TaskStatus = TaskStatus.PENDING,
    val isRest: Boolean = false,
    val repeatRule: String? = null,
    val parentGoalId: Long? = null,
    val baseExp: Int = 20,
    val earnedExp: Int = 0,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TaskStatus(val value: Int) {
    PENDING(0),
    DONE(1),
    SKIPPED(2),
    DEFERRED(3);

    companion object {
        fun fromValue(value: Int): TaskStatus =
            entries.find { it.value == value } ?: PENDING
    }
}

enum class Priority(val value: Int) {
    HIGH(0),
    MEDIUM(1),
    LOW(2);

    companion object {
        fun fromValue(value: Int): Priority =
            entries.find { it.value == value } ?: MEDIUM
    }
}

/**
 * 重复配置
 */
data class RepeatConfig(
    val configId: Long = 0,
    val planTaskId: Long = 0,
    val ruleType: RepeatRuleType = RepeatRuleType.DAILY,
    val cronExpr: String? = null,
    val intervalDays: Int = 1,
    val nextFireDate: String = "",
    val endDate: String? = null,
    val isActive: Boolean = true
)

enum class RepeatRuleType(val value: String) {
    DAILY("daily"),
    WEEKLY("weekly"),
    WEEKDAY("weekday"),
    CUSTOM("custom");

    companion object {
        fun fromValue(value: String): RepeatRuleType =
            entries.find { it.value == value } ?: DAILY
    }
}

/**
 * 长期目标
 */
data class LongTermGoal(
    val goalId: Long = 0,
    val title: String = "",
    val description: String = "",
    val attributeKey: String? = null,
    val deadline: String? = null,
    val progress: Float = 0f,
    val status: GoalStatus = GoalStatus.ACTIVE,
    val phaseData: List<GoalPhase>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class GoalStatus(val value: Int) {
    ACTIVE(0),
    COMPLETED(1),
    ABANDONED(2);

    companion object {
        fun fromValue(value: Int): GoalStatus =
            entries.find { it.value == value } ?: ACTIVE
    }
}

data class GoalPhase(
    val name: String,
    val description: String = "",
    val completed: Boolean = false,
    val estimatedDays: Int = 7
)
