package com.lingjing.feature.attribute.engine

import com.lingjing.domain.model.Attribute
import com.lingjing.domain.model.Realm
import com.lingjing.domain.repository.AttributeRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 境界计算器
 * 取五大基础属性平均等级，对应炼气期~化神期
 */
@Singleton
class RealmCalculator @Inject constructor(
    private val attributeRepository: AttributeRepository
) {

    /**
     * 获取当前境界
     */
    suspend fun getCurrentRealm(): Realm {
        val avgLevel = attributeRepository.getAverageLevel()
        return Realm.fromAverageLevel(avgLevel)
    }

    /**
     * 计算到下一境界还需提升多少平均等级
     */
    suspend fun getProgressToNextRealm(): RealmProgress? {
        val avgLevel = attributeRepository.getAverageLevel()
        val currentRealm = Realm.fromAverageLevel(avgLevel)

        val nextRealm = Realm.entries
            .filter { it.minAvgLevel > currentRealm.minAvgLevel }
            .minByOrNull { it.minAvgLevel }
            ?: return null  // 已达最高境界

        val progress = if (nextRealm.minAvgLevel > currentRealm.minAvgLevel) {
            (avgLevel - currentRealm.minAvgLevel) /
                    (nextRealm.minAvgLevel - currentRealm.minAvgLevel).toFloat()
        } else {
            0f
        }

        return RealmProgress(
            currentRealm = currentRealm,
            nextRealm = nextRealm,
            currentAvgLevel = avgLevel,
            requiredAvgLevel = nextRealm.minAvgLevel.toFloat(),
            progress = progress.coerceIn(0f, 1f)
        )
    }

    /**
     * 检查是否刚突破境界
     */
    suspend fun checkBreakthrough(oldAvgLevel: Float, newAvgLevel: Float): List<Realm> {
        val breakthroughs = mutableListOf<Realm>()
        val oldRealm = Realm.fromAverageLevel(oldAvgLevel)
        val newRealm = Realm.fromAverageLevel(newAvgLevel)

        var current = oldRealm
        while (current != newRealm) {
            val next = Realm.entries
                .filter { it.minAvgLevel > current.minAvgLevel }
                .minByOrNull { it.minAvgLevel }
                ?: break
            breakthroughs.add(next)
            current = next
        }

        return breakthroughs
    }

    /**
     * 获取境界名称（含语气词）
     */
    fun getRealmDisplayName(realm: Realm): String = when (realm) {
        Realm.QI_REFINING -> "炼气期 · 初入道途"
        Realm.FOUNDATION -> "筑基期 · 道基初成"
        Realm.GOLDEN_CORE -> "金丹期 · 丹成九转"
        Realm.NASCENT_SOUL -> "元婴期 · 元胎化婴"
        Realm.SPIRIT_TRANSFORMATION -> "化神期 · 天人合一"
    }

    data class RealmProgress(
        val currentRealm: Realm,
        val nextRealm: Realm,
        val currentAvgLevel: Float,
        val requiredAvgLevel: Float,
        val progress: Float  // 0.0 ~ 1.0
    )
}
