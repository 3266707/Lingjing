package com.lingjing.core.di.module

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.lingjing.core.common.constant.AppConstants
import com.lingjing.core.database.AppDatabase
import com.lingjing.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val TAG = "LingjingDB"

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppConstants.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .addCallback(object : androidx.room.RoomDatabase.Callback() {
                override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onCreate(db)
                    Log.i(TAG, "Database created successfully")
                }

                override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                    super.onOpen(db)
                    Log.i(TAG, "Database opened")
                }
            })
            .build()
    }

    @Provides
    @Singleton
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    @Singleton
    fun provideAttributeDao(db: AppDatabase): AttributeDao = db.attributeDao()

    @Provides
    @Singleton
    fun providePlanDao(db: AppDatabase): PlanDao = db.planDao()

    @Provides
    @Singleton
    fun provideReviewLogDao(db: AppDatabase): ReviewLogDao = db.reviewLogDao()

    @Provides
    @Singleton
    fun provideDailyStateDao(db: AppDatabase): DailyStateDao = db.dailyStateDao()

    @Provides
    @Singleton
    fun provideAchievementDao(db: AppDatabase): AchievementDao = db.achievementDao()

    @Provides
    @Singleton
    fun provideSystemDao(db: AppDatabase): SystemDao = db.systemDao()
}
