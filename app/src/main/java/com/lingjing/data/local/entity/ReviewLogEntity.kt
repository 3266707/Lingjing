package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 复盘日志实体
 */
@Entity(
    tableName = DbConstants.TABLE_REVIEW_LOGS,
    indices = [androidx.room.Index(value = ["date"])]
)
data class ReviewLogEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id") val id: Long = 0,

    @ColumnInfo(name = "date") val date: String,  // yyyy-MM-dd
    @ColumnInfo(name = "review_text") val reviewText: String = "",

    @ColumnInfo(name = "emotion") val emotion: String? = null,  // positive/neutral/negative
    @ColumnInfo(name = "emotion_detail") val emotionDetail: String? = null,

    @ColumnInfo(name = "unfinished_reason") val unfinishedReason: String? = null,
    @ColumnInfo(name = "exp_balance_comment") val expBalanceComment: String? = null,
    @ColumnInfo(name = "attribute_balance_comment") val attributeBalanceComment: String? = null,

    @ColumnInfo(name = "difficulty_shift") val difficultyShift: Float = 0f,
    @ColumnInfo(name = "tomorrow_suggestion") val tomorrowSuggestion: String? = null,
    @ColumnInfo(name = "strategy_advice") val strategyAdvice: String? = null,

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)
