package com.lingjing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lingjing.core.common.constant.DbConstants

/**
 * 用户配置键值对实体
 */
@Entity(tableName = DbConstants.TABLE_USER_CONFIG)
data class UserConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "config_key") val configKey: String,

    @ColumnInfo(name = "config_value") val configValue: String
)
