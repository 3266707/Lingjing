package com.lingjing.feature.achievement

import com.lingjing.core.database.dao.AchievementDao
import com.lingjing.core.database.dao.TaskDao
import com.lingjing.data.local.entity.AchievementDefinitions
import com.lingjing.data.local.entity.AchievementEntity
import com.lingjing.domain.model.Achievement
import com.lingjing.domain.model.Realm
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 成就检测引擎
 * 在用户操作后检测是否达成成就条件
 */
@Singleton
class AchievementEngine @Inject constructor(
    private val achievementDao: AchievementDao,
    private val taskDao: TaskDao
) {

    /**
     * 初始化所有成就
     */
    suspend fun initAchievements() {
        AchievementDefinitions.ALL.forEach { def ->
            val existing = achievementDao.getAchievement(def.key)
            if (existing == null) {
                achievementDao.insertAchievement(
                    AchievementEntity(
                        achievementKey = def.key,
                        name = def.name,
                        description = def.description,
                        conditionDescription = def.conditionDescription,
                        rewardType = def.rewardType,
                        rewardValue = def.rewardValue,
                        category = def.category,
                        displayOrder = def.displayOrder
                    )
                )
            }
        }
    }

    /**
     * 检测所有成就（在完成任务/复盘后调用）
     */
    suspend fun checkAllAchievements(
        totalCompletedTasks: Int? = null,
        consecutiveSuccessDays: Int? = null,
        currentRealm: Realm? = null,
        attributeLevels: Map<String, Int>? = null,
        hasReview: Boolean? = null,
        completionRate: Float? = null
    ): List<Achievement> {
        val unlocked = mutableListOf<Achievement>()

        val completedCount = totalCompletedTasks ?: taskDao.getTotalCompletedTasks()

        AchievementDefinitions.ALL.forEach { def ->
            val entity = achievementDao.getAchievement(def.key) ?: return@forEach
            if (entity.isUnlocked) return@forEach

            val shouldUnlock = when (def.key) {
                "seven_day_builder" -> (consecutiveSuccessDays ?: 0) >= 7 && (completionRate ?: 0f) >= 0.8f
                "golden_core_path" -> currentRealm != null &&
                        currentRealm.minAvgLevel >= Realm.GOLDEN_CORE.minAvgLevel
                "iron_will" -> (attributeLevels?.get("will") ?: 0) >= 20
                "hundred_tasks" -> completedCount >= 100
                "thirty_day_streak" -> (consecutiveSuccessDays ?: 0) >= 30
                "nascent_soul_master" -> currentRealm != null &&
                        currentRealm.minAvgLevel >= Realm.NASCENT_SOUL.minAvgLevel
                "first_review" -> hasReview == true
                "perfect_week" -> (consecutiveSuccessDays ?: 0) >= 7 && (completionRate ?: 0f) >= 1.0f
                else -> false
            }

            if (shouldUnlock) {
                achievementDao.unlockAchievement(def.key)
                unlocked.add(
                    Achievement(
                        key = def.key,
                        name = def.name,
                        description = def.description,
                        conditionDescription = def.conditionDescription,
                        isUnlocked = true,
                        unlockedAt = System.currentTimeMillis(),
                        rewardType = def.rewardType,
                        rewardValue = def.rewardValue,
                        category = def.category,
                        displayOrder = def.displayOrder
                    )
                )
            }
        }

        return unlocked
    }
}
