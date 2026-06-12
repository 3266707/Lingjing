package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.AttributeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttributeDao {

    @Query("SELECT * FROM ${DbConstants.TABLE_ATTRIBUTES} ORDER BY is_base DESC, attribute_key ASC")
    fun getAllAttributes(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_ATTRIBUTES} WHERE is_base = 1 ORDER BY attribute_key ASC")
    fun getBaseAttributes(): Flow<List<AttributeEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_ATTRIBUTES} WHERE attribute_key = :key")
    suspend fun getAttribute(key: String): AttributeEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_ATTRIBUTES} WHERE attribute_key = :key")
    fun getAttributeFlow(key: String): Flow<AttributeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttribute(attribute: AttributeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttributes(attributes: List<AttributeEntity>)

    @Update
    suspend fun updateAttribute(attribute: AttributeEntity)

    @Query("UPDATE ${DbConstants.TABLE_ATTRIBUTES} SET level = level + 1, current_exp = 0, updated_at = :updatedAt WHERE attribute_key = :key")
    suspend fun levelUpAttribute(key: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ${DbConstants.TABLE_ATTRIBUTES} SET current_exp = current_exp + :exp, total_exp_earned = total_exp_earned + :exp, updated_at = :updatedAt WHERE attribute_key = :key")
    suspend fun addExperience(key: String, exp: Long, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteAttribute(attribute: AttributeEntity)
}
