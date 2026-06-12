package com.lingjing.domain.repository

import com.lingjing.domain.model.Attribute
import com.lingjing.domain.model.Realm
import kotlinx.coroutines.flow.Flow

/**
 * 属性仓库接口
 */
interface AttributeRepository {

    fun getAllAttributes(): Flow<List<Attribute>>
    fun getBaseAttributes(): Flow<List<Attribute>>
    suspend fun getAttribute(key: String): Attribute?
    fun getAttributeFlow(key: String): Flow<Attribute?>

    suspend fun initDefaultAttributes()
    suspend fun addExperience(key: String, exp: Long): Attribute?
    suspend fun subtractExperience(key: String, exp: Long)
    suspend fun calculateRealm(): Realm
    suspend fun getAverageLevel(): Float

    suspend fun insertAttribute(attribute: Attribute)
    suspend fun updateAttribute(attribute: Attribute)
    suspend fun deleteCustomAttribute(key: String)
}
