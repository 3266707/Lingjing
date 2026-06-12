package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.PlanEntity
import com.lingjing.data.local.entity.PlanTaskEntity
import com.lingjing.data.local.entity.LongTermGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {

    // Plan operations
    @Query("SELECT * FROM ${DbConstants.TABLE_PLANS} ORDER BY date DESC, created_at DESC")
    fun getAllPlans(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_PLANS} WHERE date = :date ORDER BY created_at DESC")
    fun getPlansByDate(date: String): Flow<List<PlanEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_PLANS} WHERE plan_id = :planId")
    suspend fun getPlanById(planId: Long): PlanEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_PLANS} WHERE type = :type ORDER BY date DESC")
    suspend fun getPlansByType(type: String): List<PlanEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_PLANS} WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getPlansInRange(startDate: String, endDate: String): List<PlanEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: PlanEntity): Long

    @Transaction
    suspend fun savePlanWithTasks(plan: PlanEntity, tasks: List<PlanTaskEntity>): Long {
        val planId = insertPlan(plan)
        val tasksWithPlanId = tasks.map { it.copy(planId = planId) }
        insertPlanTasks(tasksWithPlanId)
        return planId
    }

    @Update
    suspend fun updatePlan(plan: PlanEntity)

    @Query("DELETE FROM ${DbConstants.TABLE_PLANS} WHERE plan_id = :planId")
    suspend fun deletePlan(planId: Long)

    // PlanTask operations
    @Query("SELECT * FROM ${DbConstants.TABLE_PLAN_TASKS} WHERE plan_id = :planId ORDER BY order_index ASC")
    suspend fun getPlanTasks(planId: Long): List<PlanTaskEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_PLAN_TASKS} WHERE task_id = :taskId")
    suspend fun getPlanTaskById(taskId: Long): PlanTaskEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_PLAN_TASKS} WHERE plan_id = :planId")
    fun getPlanTasksFlow(planId: Long): Flow<List<PlanTaskEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_PLAN_TASKS} WHERE status = 0 AND parent_goal_id = :goalId")
    suspend fun getPendingTasksByGoal(goalId: Long): List<PlanTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanTask(task: PlanTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanTasks(tasks: List<PlanTaskEntity>): List<Long>

    @Update
    suspend fun updatePlanTask(task: PlanTaskEntity)

    @Query("UPDATE ${DbConstants.TABLE_PLAN_TASKS} SET status = :status, completed_at = :completedAt, updated_at = :updatedAt WHERE task_id = :taskId")
    suspend fun updatePlanTaskStatus(taskId: Long, status: Int, completedAt: Long? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ${DbConstants.TABLE_PLAN_TASKS} SET earned_exp = :earnedExp, updated_at = :updatedAt WHERE task_id = :taskId")
    suspend fun updatePlanTaskExp(taskId: Long, earnedExp: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM ${DbConstants.TABLE_PLAN_TASKS} WHERE task_id = :taskId")
    suspend fun deletePlanTask(taskId: Long)

    // Combined: plan with tasks
    @Transaction
    @Query("SELECT * FROM ${DbConstants.TABLE_PLANS} WHERE plan_id = :planId")
    suspend fun getPlanWithTasks(planId: Long): PlanWithTasks?

    // Long-term goal operations
    @Query("SELECT * FROM ${DbConstants.TABLE_LONG_TERM_GOALS} ORDER BY status ASC, deadline ASC")
    fun getAllGoals(): Flow<List<LongTermGoalEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_LONG_TERM_GOALS} WHERE goal_id = :goalId")
    suspend fun getGoalById(goalId: Long): LongTermGoalEntity?

    @Insert
    suspend fun insertGoal(goal: LongTermGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: LongTermGoalEntity)

    @Query("DELETE FROM ${DbConstants.TABLE_LONG_TERM_GOALS} WHERE goal_id = :goalId")
    suspend fun deleteGoal(goalId: Long)
}

/**
 * 计划与任务关联查询结果
 */
data class PlanWithTasks(
    @Embedded val plan: PlanEntity,
    @Relation(
        parentColumn = "plan_id",
        entityColumn = "plan_id"
    )
    val tasks: List<PlanTaskEntity>
)
