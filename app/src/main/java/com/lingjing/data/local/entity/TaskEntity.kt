package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 任务实体
 */
@Entity(
    tableName = DbConstants.TABLE_TASKS,
    foreignKeys = [
        ForeignKey(
            entity = PlanEntity::class,
            parentColumns = ["plan_id"],
            childColumns = ["plan_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["plan_id"]),
        Index(value = ["status"]),
        Index(value = ["date", "status"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "task_id") val taskId: Long = 0,

    @ColumnInfo(name = "plan_id") val planId: Long? = null,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String = "",

    @ColumnInfo(name = "attribute_key") val attributeKey: String,  // wisdom, physique, etc.
    @ColumnInfo(name = "estimated_minutes") val estimatedMinutes: Int = 30,
    @ColumnInfo(name = "actual_minutes") val actualMinutes: Int? = null,

    @ColumnInfo(name = "difficulty_score") val difficultyScore: Int = 3,  // 1-5
    @ColumnInfo(name = "priority") val priority: Int = 1,  // 0=高, 1=中, 2=低
    @ColumnInfo(name = "order_index") val orderIndex: Int = 0,

    @ColumnInfo(name = "status") val status: Int = DbConstants.TASK_PENDING,

    @ColumnInfo(name = "is_rest") val isRest: Boolean = false,
    @ColumnInfo(name = "repeat_rule") val repeatRule: String? = null,  // daily/weekly/cron

    @ColumnInfo(name = "base_exp") val baseExp: Int = 20,
    @ColumnInfo(name = "earned_exp") val earnedExp: Int = 0,

    @ColumnInfo(name = "date") val date: String,  // yyyy-MM-dd
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
