package com.lingjing.data.repository.impl

import com.lingjing.core.common.constant.DbConstants
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.core.database.dao.PlanDao
import com.lingjing.core.database.dao.TaskDao
import com.lingjing.data.local.entity.*
import com.lingjing.data.mapper.PlanMapper
import com.lingjing.domain.model.*
import com.lingjing.domain.repository.AttributeRepository
import com.lingjing.domain.repository.DailyStateRepository
import com.lingjing.domain.repository.PlanRepository
import com.lingjing.feature.attribute.engine.ExperienceCalculationService
import com.lingjing.feature.energy.engine.ZhenyuanStateManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanRepositoryImpl @Inject constructor(
    private val planDao: PlanDao,
    private val taskDao: TaskDao,
    private val mapper: PlanMapper,
    private val attributeRepository: AttributeRepository,
    private val expCalculationService: ExperienceCalculationService,
    private val dailyStateRepository: DailyStateRepository,
    private val zhenyuanStateManager: ZhenyuanStateManager
) : PlanRepository {

    override fun getAllPlans(): Flow<List<Plan>> {
        return planDao.getAllPlans().map { entities ->
            entities.map { mapper.planEntityToDomain(it) }
        }
    }

    override fun getPlansByDate(date: String): Flow<List<Plan>> {
        return planDao.getPlansByDate(date).map { entities ->
            entities.map { mapper.planEntityToDomain(it) }
        }
    }

    override suspend fun getPlanById(planId: Long): Plan? {
        return planDao.getPlanWithTasks(planId)?.let {
            mapper.planWithTasksToDomain(it)
        }
    }

    override suspend fun getPlansByType(type: PlanType): List<Plan> {
        return planDao.getPlansByType(type.value).map { mapper.planEntityToDomain(it) }
    }

    override suspend fun getPlansInRange(startDate: String, endDate: String): List<Plan> {
        return planDao.getPlansInRange(startDate, endDate).map { mapper.planEntityToDomain(it) }
    }

    override suspend fun getPlanWithTasks(planId: Long): Plan? {
        return planDao.getPlanWithTasks(planId)?.let { mapper.planWithTasksToDomain(it) }
    }

    override suspend fun insertPlan(plan: Plan): Long {
        return planDao.insertPlan(mapper.planDomainToEntity(plan))
    }

    override suspend fun updatePlan(plan: Plan) {
        planDao.updatePlan(mapper.planDomainToEntity(plan))
    }

    override suspend fun deletePlan(planId: Long) {
        planDao.deletePlan(planId)
    }

    override suspend fun getPlanTasks(planId: Long): List<PlanTask> {
        return planDao.getPlanTasks(planId).map { mapper.planTaskEntityToDomain(it) }
    }

    override fun getPlanTasksFlow(planId: Long): Flow<List<PlanTask>> {
        return planDao.getPlanTasksFlow(planId).map { entities ->
            entities.map { mapper.planTaskEntityToDomain(it) }
        }
    }

    override suspend fun getPlanTaskById(taskId: Long): PlanTask? {
        return planDao.getPlanTaskById(taskId)?.let { mapper.planTaskEntityToDomain(it) }
    }

    override suspend fun insertPlanTask(task: PlanTask): Long {
        return planDao.insertPlanTask(mapper.planTaskDomainToEntity(task))
    }

    override suspend fun insertPlanTasks(tasks: List<PlanTask>): List<Long> {
        return planDao.insertPlanTasks(tasks.map { mapper.planTaskDomainToEntity(it) })
    }

    override suspend fun updatePlanTask(task: PlanTask) {
        planDao.updatePlanTask(mapper.planTaskDomainToEntity(task))
    }

    override suspend fun updateTaskStatus(taskId: Long, status: TaskStatus, completedAt: Long?) {
        planDao.updatePlanTaskStatus(taskId, status.value, completedAt)
    }

    override suspend fun deletePlanTask(taskId: Long) {
        planDao.deletePlanTask(taskId)
    }

    override suspend fun saveGeneratedPlan(plan: Plan, tasks: List<PlanTask>): Long {
        return planDao.savePlanWithTasks(
            mapper.planDomainToEntity(plan),
            tasks.map { mapper.planTaskDomainToEntity(it) }
        )
    }

    override suspend fun getTodayTasks(): List<PlanTask> {
        val today = DateTimeUtils.today()
        val planEntities = planDao.getPlansInRange(today, today)
        if (planEntities.isEmpty()) return emptyList()

        return planEntities.flatMap { plan ->
            planDao.getPlanTasks(plan.planId).map { mapper.planTaskEntityToDomain(it) }
        }
    }

    override suspend fun completeTask(taskId: Long): PlanTask? {
        val task = planDao.getPlanTaskById(taskId) ?: return null
        if (task.status != DbConstants.TASK_PENDING) return null

        val now = System.currentTimeMillis()
        val today = DateTimeUtils.today()

        // 1. Update status to DONE
        planDao.updatePlanTaskStatus(taskId, DbConstants.TASK_DONE, now)

        // 2. Get actual qi concentration from today's daily state
        val dailyState = dailyStateRepository.getOrCreateDailyState(today)
        val qi = dailyState.qiConcentration

        // 3. Calculate earned experience with actual qi
        val expInput = ExperienceCalculationService.ExpInput(
            baseExp = task.baseExp,
            difficultyScore = task.difficultyScore,
            qualityScore = 3,
            estimatedMinutes = task.estimatedMinutes,
            actualMinutes = null,
            qiConcentration = qi
        )
        val earnedExp = expCalculationService.calculate(expInput).toInt()

        // 4. Apply experience to the task's associated attribute
        attributeRepository.addExperience(task.attributeKey, earnedExp.toLong())

        // 5. Update task's earned experience field
        planDao.updatePlanTaskExp(taskId, earnedExp, now)

        // 6. Consume energy
        val energyCost = zhenyuanStateManager.calculateTaskCost(task.estimatedMinutes)
        val updatedState = dailyState.copy(
            zhenyuanUsed = dailyState.zhenyuanUsed + energyCost,
            completedTasks = dailyState.completedTasks + 1,
            totalTasks = maxOf(dailyState.totalTasks, dailyState.completedTasks + 1),
            updatedAt = now
        )
        dailyStateRepository.updateDailyState(updatedState)

        // 7. Recalculate qi concentration
        dailyStateRepository.calculateAndUpdateQiConcentration(today)

        // 8. Return updated task
        return planDao.getPlanTaskById(taskId)?.let { mapper.planTaskEntityToDomain(it) }
    }

    override suspend fun revertTask(taskId: Long) {
        val task = planDao.getPlanTaskById(taskId) ?: return
        if (task.status != DbConstants.TASK_DONE) return

        planDao.updatePlanTaskStatus(
            taskId,
            DbConstants.TASK_PENDING,
            null  // Clear completedAt
        )
    }
}
