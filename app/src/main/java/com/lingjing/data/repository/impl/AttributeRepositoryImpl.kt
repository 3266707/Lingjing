package com.lingjing.data.repository.impl

import com.lingjing.core.common.constant.DbConstants
import com.lingjing.core.database.dao.AttributeDao
import com.lingjing.data.local.entity.AttributeEntity
import com.lingjing.domain.model.Attribute
import com.lingjing.domain.model.Realm
import com.lingjing.domain.repository.AttributeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttributeRepositoryImpl @Inject constructor(
    private val attributeDao: AttributeDao
) : AttributeRepository {

    override fun getAllAttributes(): Flow<List<Attribute>> {
        return attributeDao.getAllAttributes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getBaseAttributes(): Flow<List<Attribute>> {
        return attributeDao.getBaseAttributes().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAttribute(key: String): Attribute? {
        return attributeDao.getAttribute(key)?.toDomain()
    }

    override fun getAttributeFlow(key: String): Flow<Attribute?> {
        return attributeDao.getAttributeFlow(key).map { it?.toDomain() }
    }

    override suspend fun initDefaultAttributes() {
        val defaults = listOf(
            AttributeEntity(
                attributeKey = DbConstants.ATTR_WISDOM,
                name = "灵根·悟性",
                level = 1,
                currentExp = 0,
                isBase = true,
                weight = 1.0f
            ),
            AttributeEntity(
                attributeKey = DbConstants.ATTR_PHYSIQUE,
                name = "道体·体魄",
                level = 1,
                currentExp = 0,
                isBase = true,
                weight = 1.0f
            ),
            AttributeEntity(
                attributeKey = DbConstants.ATTR_PERCEPTION,
                name = "神识·感知",
                level = 1,
                currentExp = 0,
                isBase = true,
                weight = 1.0f
            ),
            AttributeEntity(
                attributeKey = DbConstants.ATTR_ENERGY,
                name = "真元·精力",
                level = 1,
                currentExp = 0,
                isBase = true,
                weight = 1.0f
            ),
            AttributeEntity(
                attributeKey = DbConstants.ATTR_WILL,
                name = "丹心·意志",
                level = 1,
                currentExp = 0,
                isBase = true,
                weight = 1.0f
            )
        )
        attributeDao.insertAttributes(defaults)
    }

    override suspend fun addExperience(key: String, exp: Long): Attribute? {
        attributeDao.getAttribute(key) ?: return null
        attributeDao.addExperience(key, exp)

        // 检查是否升级
        val updated = attributeDao.getAttribute(key) ?: return null
        val expNeeded = updated.level.toLong() * updated.level * 100
        if (updated.currentExp >= expNeeded) {
            attributeDao.levelUpAttribute(key)
            return attributeDao.getAttribute(key)?.toDomain()
        }
        return updated.toDomain()
    }

    override suspend fun subtractExperience(key: String, exp: Long) {
        val attr = attributeDao.getAttribute(key) ?: return
        val newExp = (attr.currentExp - exp).coerceAtLeast(0)
        attributeDao.addExperience(key, -exp.coerceAtMost(attr.currentExp.toLong()))
    }

    override suspend fun calculateRealm(): Realm {
        val avgLevel = getAverageLevel()
        return Realm.fromAverageLevel(avgLevel)
    }

    override suspend fun getAverageLevel(): Float {
        return try {
            // Direct calculation from known keys
            val keys = DbConstants.BASE_ATTRIBUTES
            var totalLevel = 0
            var count = 0
            for (key in keys) {
                val attr = attributeDao.getAttribute(key)
                if (attr != null) {
                    totalLevel += attr.level
                    count++
                }
            }
            if (count > 0) totalLevel.toFloat() / count else 0f
        } catch (e: Exception) {
            0f
        }
    }

    override suspend fun insertAttribute(attribute: Attribute) {
        attributeDao.insertAttribute(attribute.toEntity())
    }

    override suspend fun updateAttribute(attribute: Attribute) {
        attributeDao.updateAttribute(attribute.toEntity())
    }

    override suspend fun deleteCustomAttribute(key: String) {
        val attr = attributeDao.getAttribute(key) ?: return
        if (!attr.isBase) {
            attributeDao.deleteAttribute(attr)
        }
    }
}

// Extension functions for mapping
private fun AttributeEntity.toDomain(): Attribute = Attribute(
    key = attributeKey,
    name = name,
    level = level,
    currentExp = currentExp,
    totalExpEarned = totalExpEarned,
    isBase = isBase,
    weight = weight,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Attribute.toEntity(): AttributeEntity = AttributeEntity(
    attributeKey = key,
    name = name,
    level = level,
    currentExp = currentExp,
    totalExpEarned = totalExpEarned,
    isBase = isBase,
    weight = weight,
    createdAt = createdAt,
    updatedAt = updatedAt
)
