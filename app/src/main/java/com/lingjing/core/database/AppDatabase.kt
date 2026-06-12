package com.lingjing.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lingjing.core.common.constant.AppConstants
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.core.database.dao.*
import com.lingjing.data.local.entity.*

@Database(
    entities = [
        // 被外键引用的父表必须排在子表前面
        AttributeEntity::class,
        ReviewLogEntity::class,
        DailyStateEntity::class,
        AchievementEntity::class,
        UserConfigEntity::class,
        PlanEntity::class,
        PlanTaskEntity::class,
        LongTermGoalEntity::class,
        TaskEntity::class,              // FK → PlanEntity
        RepeatTaskConfigEntity::class,  // FK → PlanTaskEntity
        PlanTemplateEntity::class,
        TaskEmbeddingEntity::class,
        OfflineRequestEntity::class,
        RestTaskInstanceEntity::class,
        EnergyTransactionEntity::class,
        MemoryEntryEntity::class
    ],
    version = AppConstants.DATABASE_VERSION,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskDao(): TaskDao
    abstract fun attributeDao(): AttributeDao
    abstract fun planDao(): PlanDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun dailyStateDao(): DailyStateDao
    abstract fun achievementDao(): AchievementDao
    abstract fun systemDao(): SystemDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Remove unique constraint on review_logs.date to allow multiple entries per day
                db.execSQL("DROP INDEX IF EXISTS index_${DbConstants.TABLE_REVIEW_LOGS}_date")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_${DbConstants.TABLE_REVIEW_LOGS}_date ON ${DbConstants.TABLE_REVIEW_LOGS}(date)")
            }
        }
    }
}
