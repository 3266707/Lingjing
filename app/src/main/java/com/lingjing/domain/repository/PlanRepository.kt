package com.lingjing.domain.repository

import com.lingjing.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * 计划仓库接口
 */
interface PlanRepository {

    // 计划
    fun getAllPlans(): Flow<List<Plan>>

    fun getPlansByDate(date: String): Flow<List<Plan>>
    suspend fun getPlanById(planId: Long): Plan?
    suspend fun getPlansByType(type: PlanType): List<Plan>
    suspend fun getPlansInRange(startDate: String, endDate: String): List<Plan>
    suspend fun getPlanWithTasks(planId: Long): Plan?

    suspend fun insertPlan(plan: Plan): Long
    suspend fun updatePlan(plan: Plan)
    suspend fun deletePlan(planId: Long)

    // 任务
    suspend fun getPlanTasks(planId: Long): List<PlanTask>
    fun getPlanTasksFlow(planId: Long): Flow<List<PlanTask>>
    suspend fun getPlanTaskById(taskId: Long): PlanTask?

    suspend fun insertPlanTask(task: PlanTask): Long
    suspend fun insertPlanTasks(tasks: List<PlanTask>): List<Long>
    suspend fun updatePlanTask(task: PlanTask)
    suspend fun updateTaskStatus(taskId: Long, status: TaskStatus, completedAt: Long? = null)
    suspend fun deletePlanTask(taskId: Long)

    // 生成计划（包含计划 + 任务一起保存）
    suspend fun saveGeneratedPlan(plan: Plan, tasks: List<PlanTask>): Long

    // 每日任务（映射到今日看板）
    suspend fun getTodayTasks(): List<PlanTask>
    suspend fun completeTask(taskId: Long): PlanTask?
    suspend fun revertTask(taskId: Long)
}
