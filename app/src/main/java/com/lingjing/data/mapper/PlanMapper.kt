package com.lingjing.data.mapper

import com.lingjing.core.database.dao.PlanWithTasks
import com.lingjing.data.local.entity.*
import com.lingjing.domain.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanMapper @Inject constructor() {

    // Plan
    fun planEntityToDomain(entity: PlanEntity): Plan = Plan(
        planId = entity.planId,
        type = PlanType.fromValue(entity.type),
        title = entity.title,
        sourceText = entity.sourceText,
        date = entity.date,
        isTemplate = entity.isTemplate,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun planDomainToEntity(domain: Plan): PlanEntity = PlanEntity(
        planId = domain.planId,
        type = domain.type.value,
        title = domain.title,
        sourceText = domain.sourceText,
        date = domain.date,
        isTemplate = domain.isTemplate,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )

    // PlanTask
    fun planTaskEntityToDomain(entity: PlanTaskEntity): PlanTask = PlanTask(
        taskId = entity.taskId,
        planId = entity.planId,
        name = entity.name,
        attributeKey = entity.attributeKey,
        estimatedMinutes = entity.estimatedMinutes,
        actualMinutes = entity.actualMinutes,
        difficultyScore = entity.difficultyScore,
        priority = Priority.fromValue(entity.priority),
        orderIndex = entity.orderIndex,
        status = TaskStatus.fromValue(entity.status),
        isRest = entity.isRest,
        repeatRule = entity.repeatRule,
        parentGoalId = entity.parentGoalId,
        baseExp = entity.baseExp,
        earnedExp = entity.earnedExp,
        completedAt = entity.completedAt,
        createdAt = entity.createdAt
    )

    fun planTaskDomainToEntity(domain: PlanTask): PlanTaskEntity = PlanTaskEntity(
        taskId = domain.taskId,
        planId = domain.planId,
        name = domain.name,
        attributeKey = domain.attributeKey,
        estimatedMinutes = domain.estimatedMinutes,
        actualMinutes = domain.actualMinutes,
        difficultyScore = domain.difficultyScore,
        priority = domain.priority.value,
        orderIndex = domain.orderIndex,
        status = domain.status.value,
        isRest = domain.isRest,
        repeatRule = domain.repeatRule,
        parentGoalId = domain.parentGoalId,
        baseExp = domain.baseExp,
        earnedExp = domain.earnedExp,
        completedAt = domain.completedAt,
        createdAt = domain.createdAt
    )

    // PlanWithTasks
    fun planWithTasksToDomain(planWithTasks: PlanWithTasks): Plan {
        return planEntityToDomain(planWithTasks.plan).copy(
            tasks = planWithTasks.tasks.map { planTaskEntityToDomain(it) }
        )
    }

    // LongTermGoal
    fun goalEntityToDomain(entity: LongTermGoalEntity): LongTermGoal = LongTermGoal(
        goalId = entity.goalId,
        title = entity.title,
        description = entity.description,
        attributeKey = entity.attributeKey,
        deadline = entity.deadline,
        progress = entity.progress,
        status = GoalStatus.fromValue(entity.status),
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt
    )

    fun goalDomainToEntity(domain: LongTermGoal): LongTermGoalEntity = LongTermGoalEntity(
        goalId = domain.goalId,
        title = domain.title,
        description = domain.description,
        attributeKey = domain.attributeKey,
        deadline = domain.deadline,
        progress = domain.progress,
        status = domain.status.value,
        createdAt = domain.createdAt,
        updatedAt = domain.updatedAt
    )
}
