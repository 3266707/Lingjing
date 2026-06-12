package com.lingjing.core.database.dao

import androidx.room.*
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM ${DbConstants.TABLE_TASKS} WHERE date = :date ORDER BY order_index ASC")
    fun getTasksByDate(date: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM ${DbConstants.TABLE_TASKS} WHERE task_id = :taskId")
    suspend fun getTaskById(taskId: Long): TaskEntity?

    @Query("SELECT * FROM ${DbConstants.TABLE_TASKS} WHERE plan_id = :planId ORDER BY order_index ASC")
    suspend fun getTasksByPlanId(planId: Long): List<TaskEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_TASKS} WHERE status = :status ORDER BY date DESC")
    suspend fun getTasksByStatus(status: Int): List<TaskEntity>

    @Query("SELECT * FROM ${DbConstants.TABLE_TASKS} WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getTasksInDateRange(startDate: String, endDate: String): List<TaskEntity>

    @Query("SELECT COUNT(*) FROM ${DbConstants.TABLE_TASKS} WHERE status = ${DbConstants.TASK_DONE}")
    suspend fun getTotalCompletedTasks(): Int

    @Query("SELECT COUNT(*) FROM ${DbConstants.TABLE_TASKS} WHERE date = :date AND status = ${DbConstants.TASK_DONE}")
    suspend fun getCompletedTasksOnDate(date: String): Int

    @Query("SELECT COUNT(*) FROM ${DbConstants.TABLE_TASKS} WHERE date = :date")
    suspend fun getTotalTasksOnDate(date: String): Int

    @Query("SELECT * FROM ${DbConstants.TABLE_TASKS} WHERE status = ${DbConstants.TASK_DONE} AND date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getCompletedTasksInRange(startDate: String, endDate: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>): List<Long>

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("UPDATE ${DbConstants.TABLE_TASKS} SET status = :status, completed_at = :completedAt, updated_at = :updatedAt WHERE task_id = :taskId")
    suspend fun updateTaskStatus(taskId: Long, status: Int, completedAt: Long? = null, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE ${DbConstants.TABLE_TASKS} SET order_index = :newOrder WHERE task_id = :taskId")
    suspend fun updateTaskOrder(taskId: Long, newOrder: Int)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("DELETE FROM ${DbConstants.TABLE_TASKS} WHERE task_id = :taskId")
    suspend fun deleteTaskById(taskId: Long)
}
