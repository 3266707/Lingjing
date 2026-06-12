package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM ${DbConstants.TABLE_ACHIEVEMENTS} ORDER BY display_order ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_ACHIEVEMENTS} WHERE achievement_key = :key")
    suspend fun getAchievement(key: String): AchievementEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_ACHIEVEMENTS} WHERE is_unlocked = 1 ORDER BY unlocked_at DESC")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT COUNT(*) FROM ${DbConstants.TABLE_ACHIEVEMENTS} WHERE is_unlocked = 1")
    suspend fun getUnlockedCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(achievements: List<AchievementEntity>)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE ${DbConstants.TABLE_ACHIEVEMENTS} SET is_unlocked = 1, unlocked_at = :unlockedAt WHERE achievement_key = :key")
    suspend fun unlockAchievement(key: String, unlockedAt: Long = System.currentTimeMillis())
}
