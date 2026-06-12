package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 每日状态实体 - 记录每天的综合状态
 */
@Entity(
    tableName = DbConstants.TABLE_DAILY_STATE,
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyStateEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "date") val date: String,  // yyyy-MM-dd

    @ColumnInfo(name = "completion_rate") val completionRate: Float = 0f,
    @ColumnInfo(name = "total_tasks") val totalTasks: Int = 0,
    @ColumnInfo(name = "completed_tasks") val completedTasks: Int = 0,

    @ColumnInfo(name = "consecutive_success_days") val consecutiveSuccessDays: Int = 0,
    @ColumnInfo(name = "consecutive_fail_days") val consecutiveFailDays: Int = 0,

    @ColumnInfo(name = "zhenyuan_used") val zhenyuanUsed: Int = 0,
    @ColumnInfo(name = "zhenyuan_recovered") val zhenyuanRecovered: Int = 0,
    @ColumnInfo(name = "sleep_quality") val sleepQuality: Int? = null,  // 1-5

    @ColumnInfo(name = "avg_difficulty_score") val avgDifficultyScore: Float = 0f,
    @ColumnInfo(name = "qi_concentration") val qiConcentration: Int = 10,
    @ColumnInfo(name = "emotion") val emotion: String? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
)
