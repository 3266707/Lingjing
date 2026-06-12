package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 属性实体 - 存储五大基础属性和自定义属性
 */
@Entity(tableName = DbConstants.TABLE_ATTRIBUTES)
data class AttributeEntity(
    @PrimaryKey
    @ColumnInfo(name = "attribute_key") val attributeKey: String,  // wisdom, physique, etc.

    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "level") val level: Int = 1,
    @ColumnInfo(name = "current_exp") val currentExp: Long = 0,
    @ColumnInfo(name = "total_exp_earned") val totalExpEarned: Long = 0,

    @ColumnInfo(name = "is_base") val isBase: Boolean = true,  // 是否五大基础属性
    @ColumnInfo(name = "weight") val weight: Float = 1.0f,    // 境界计算权重

    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 计算升级到下一级所需经验: 等级² × 100
     */
    fun expToNextLevel(): Long {
        return (level.toLong() * level * 100)
    }

    /**
     * 获取经验进度 (0.0 ~ 1.0)
     */
    fun getProgress(): Float {
        val required = expToNextLevel()
        return if (required > 0) (currentExp.toFloat() / required).coerceIn(0f, 1f) else 1f
    }
}
