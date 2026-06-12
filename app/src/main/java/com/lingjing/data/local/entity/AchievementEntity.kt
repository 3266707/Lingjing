package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 成就定义实体
 */
@Entity(tableName = DbConstants.TABLE_ACHIEVEMENTS)
data class AchievementEntity(
    @PrimaryKey
    @ColumnInfo(name = "achievement_key") val achievementKey: String,

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "condition_description") val conditionDescription: String,

    @ColumnInfo(name = "is_unlocked") val isUnlocked: Boolean = false,
    @ColumnInfo(name = "unlocked_at") val unlockedAt: Long? = null,

    @ColumnInfo(name = "reward_type") val rewardType: String?,  // exp_bonus, temp_buff, plan_exemption
    @ColumnInfo(name = "reward_value") val rewardValue: Float = 0f,

    @ColumnInfo(name = "category") val category: String = "general",  // general, streak, attribute, realm
    @ColumnInfo(name = "display_order") val displayOrder: Int = 0
)
