package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 计划实体
 */
@Entity(
    tableName = DbConstants.TABLE_PLANS,
    indices = [Index(value = ["date"])]
)
data class PlanEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "plan_id") val planId: Long = 0,

    @ColumnInfo(name = "type") val type: String,  // daily, long_term, repeat
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "source_text") val sourceText: String = "",  // 用户原始输入

    @ColumnInfo(name = "date") val date: String,  // yyyy-MM-dd
    @ColumnInfo(name = "is_template") val isTemplate: Boolean = false,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 计划任务实体
 */
@Entity(
    tableName = DbConstants.TABLE_PLAN_TASKS,
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["plan_id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["plan_id"]), Index(value = ["status"]), Index(value = ["parent_goal_id"])]
)
data class PlanTaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id") val taskId: Long = 0,

    @ColumnInfo(name = "plan_id") val planId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "attribute_key") val attributeKey: String,
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int = 30,
    @ColumnInfo(name = "actual_minutes") val actualMinutes: Int? = null,

    @ColumnInfo(name = "difficulty_score") val difficultyScore: Int = 3,
    @ColumnInfo(name = "priority") val priority: Int = 1,
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,

    @ColumnInfo(name = "status") val status: Int = 0,
    @ColumnInfo(name = "is_rest") val isRest: Boolean = false,

    @ColumnInfo(name = "repeat_rule") val repeatRule: String? = null,
    @ColumnInfo(name = "parent_goal_id") val parentGoalId: Long? = null,

    @ColumnInfo(name = "base_exp") val baseExp: Int = 20,
    @ColumnInfo(name = "earned_exp") val earnedExp: Int = 0,

    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)

/**
 * 重复任务配置
 */
@Entity(
    tableName = DbConstants.TABLE_REPEAT_TASK_CONFIGS,
    foreignKeys = [
        ForeignKey(
            entity = PlanTaskEntity::class,
            parentColumns = ["task_id"],
            childColumns = ["plan_task_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["plan_task_id"])]
)
data class RepeatTaskConfigEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "config_id") val configId: Long = 0,

    @ColumnInfo(name = "plan_task_id") val planTaskId: Long,
    @ColumnInfo(name = "rule_type") val ruleType: String,  // daily, weekly, weekday, custom
    @ColumnInfo(name = "cron_expr") val cronExpr: String? = null,
    @ColumnInfo(name = "interval_days") val intervalDays: Int = 1,

    @ColumnInfo(name = "next_fire_date") val nextFireDate: String,
    @ColumnInfo(name = "end_date") val endDate: String? = null,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
)

/**
 * 计划模板
 */
@Entity(tableName = DbConstants.TABLE_PLAN_TEMPLATES)
data class PlanTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "template_id") val templateId: Long = 0,

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "plan_json") val planJson: String,  // 序列化的计划快照
    @ColumnInfo(name = "use_count") val useCount: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

/**
 * 长期目标实体 (增强版)
 */
@Entity(tableName = DbConstants.TABLE_LONG_TERM_GOALS)
data class LongTermGoalEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "goal_id") val goalId: Long = 0,

    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "description") val description: String = "",
    @ColumnInfo(name = "attribute_key") val attributeKey: String? = null,

    @ColumnInfo(name = "deadline") val deadline: String? = null,
    @ColumnInfo(name = "progress") val progress: Float = 0f,
    @ColumnInfo(name = "status") val status: Int = 0,  // 0=active, 1=completed, 2=abandoned

    @ColumnInfo(name = "phase_data") val phaseData: String? = null,  // JSON array of phases
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
