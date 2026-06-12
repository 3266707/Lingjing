package com.lingjing.ui.components

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.domain.model.*
import com.lingjing.domain.repository.AttributeRepository
import com.lingjing.domain.repository.DailyStateRepository
import com.lingjing.domain.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayBoardState(
    val date: String = DateTimeUtils.today(),
    val realm: Realm = Realm.QI_REFINING,
    val qiConcentration: Int = 10,
    val todayTasks: List<PlanTask> = emptyList(),
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val completionRate: Float = 0f,
    val isLoading: Boolean = true,
    val error: String? = null,
    val energy: Int = 100,
    val maxEnergy: Int = 100,
    val lastCompletedTaskId: Long? = null,
    val lastCompletedAttrKey: String? = null,
    val lastCompletedExp: Int = 0
)

@HiltViewModel
class TodayBoardViewModel @Inject constructor(
    private val planRepository: PlanRepository,
    private val attributeRepository: AttributeRepository,
    private val dailyStateRepository: DailyStateRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TodayBoardVM"
    }

    private val _state = MutableStateFlow(TodayBoardState())
    val state: StateFlow<TodayBoardState> = _state.asStateFlow()

    init {
        loadTodayData()
        ensureRecurringTasksForToday()
    }

    fun loadTodayData() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val today = DateTimeUtils.today()

                // 加载今日任务
                val tasks = try {
                    planRepository.getTodayTasks()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load tasks", e)
                    emptyList()
                }

                // 加载境界
                val realm = try {
                    attributeRepository.calculateRealm()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to calculate realm", e)
                    Realm.QI_REFINING
                }

                // 加载每日状态
                val dailyState = try {
                    dailyStateRepository.getOrCreateDailyState(today)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get/create daily state", e)
                    null
                }

                val qi = dailyState?.qiConcentration ?: 10

                // 计算真元
                val physiqueLevel = try {
                    attributeRepository.getAttribute("physique")?.level ?: 1
                } catch (e: Exception) {
                    1
                }
                val maxEnergy = 100 + physiqueLevel * 5 + (dailyState?.sleepQuality ?: 0) * 2
                val energyUsed = dailyState?.zhenyuanUsed ?: 0
                val currentEnergy = maxOf(0, maxEnergy - energyUsed)

                _state.update {
                    it.copy(
                        isLoading = false,
                        realm = realm,
                        qiConcentration = qi,
                        todayTasks = tasks,
                        completedTasks = tasks.count { t -> t.status == TaskStatus.DONE },
                        totalTasks = tasks.size,
                        completionRate = if (tasks.isNotEmpty())
                            tasks.count { t -> t.status == TaskStatus.DONE }.toFloat() / tasks.size
                        else 0f,
                        energy = currentEnergy,
                        maxEnergy = maxEnergy,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error loading today data", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message?.take(100) ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun completeTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val task = planRepository.getPlanTaskById(taskId) ?: return@launch
                val completed = planRepository.completeTask(taskId)
                val exp = completed?.earnedExp ?: task.baseExp

                // Optimistic local update: mark task done without full reload
                val updatedTasks = _state.value.todayTasks.map { t ->
                    if (t.taskId == taskId) t.copy(status = TaskStatus.DONE, earnedExp = exp) else t
                }
                val doneCount = updatedTasks.count { it.status == TaskStatus.DONE }
                _state.update {
                    it.copy(
                        todayTasks = updatedTasks,
                        completedTasks = doneCount,
                        completionRate = doneCount.toFloat() / updatedTasks.size,
                        lastCompletedTaskId = taskId,
                        lastCompletedAttrKey = task.attributeKey,
                        lastCompletedExp = exp
                    )
                }
                // Don't call loadTodayData() - keep Snackbar visible
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete task", e)
                _state.update { it.copy(error = "完成任务失败: ${e.message}") }
            }
        }
    }

    fun revertLastComplete() {
        val taskId = _state.value.lastCompletedTaskId ?: return
        val attrKey = _state.value.lastCompletedAttrKey ?: return
        val exp = _state.value.lastCompletedExp
        viewModelScope.launch {
            try {
                planRepository.revertTask(taskId)
                if (exp > 0) {
                    attributeRepository.subtractExperience(attrKey, exp.toLong())
                }
                _state.update { it.copy(lastCompletedTaskId = null, lastCompletedAttrKey = null, lastCompletedExp = 0) }
                // Reload to get correct server state after revert
                loadTodayData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to revert task", e)
                _state.update { it.copy(error = "撤销失败: ${e.message}") }
            }
        }
    }

    fun dismissUndo() {
        _state.update { it.copy(lastCompletedTaskId = null, lastCompletedAttrKey = null, lastCompletedExp = 0) }
        loadTodayData()
    }

    fun undoSpecificTask(taskId: Long) {
        viewModelScope.launch {
            try {
                val task = planRepository.getPlanTaskById(taskId) ?: return@launch
                if (task.status != TaskStatus.DONE) return@launch
                planRepository.revertTask(taskId)
                if (task.earnedExp > 0) {
                    attributeRepository.subtractExperience(task.attributeKey, task.earnedExp.toLong())
                }
                loadTodayData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to undo task $taskId", e)
            }
        }
    }

    fun skipTask(taskId: Long) {
        viewModelScope.launch {
            try {
                planRepository.updateTaskStatus(taskId, TaskStatus.SKIPPED)
                loadTodayData()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to skip task", e)
                _state.update { it.copy(error = "跳过任务失败: ${e.message}") }
            }
        }
    }

    fun addManualTask(task: PlanTask, targetDate: String = DateTimeUtils.today()) {
        viewModelScope.launch {
            try {
                val existingPlans = planRepository.getPlansInRange(targetDate, targetDate)
                val targetPlan = existingPlans.firstOrNull()

                val planId: Long
                if (targetPlan != null) {
                    planId = targetPlan.planId
                } else {
                    val title = if (targetDate == DateTimeUtils.today()) "今日修炼" else "${targetDate}修炼"
                    planId = planRepository.insertPlan(
                        Plan(type = PlanType.DAILY, title = title, date = targetDate)
                    )
                }

                val existingTasks = planRepository.getPlanTasks(planId)
                val maxOrder = existingTasks.maxOfOrNull { it.orderIndex } ?: -1

                planRepository.insertPlanTask(
                    task.copy(planId = planId, orderIndex = maxOrder + 1)
                )

                // Only reload if the task was added to today
                if (targetDate == DateTimeUtils.today()) {
                    loadTodayData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add manual task", e)
                _state.update { it.copy(error = "添加任务失败: ${e.message}") }
            }
        }
    }

    /**
     * Generate today's instances of recurring tasks from previous days.
     * Called once on app startup.
     */
    fun ensureRecurringTasksForToday() {
        viewModelScope.launch {
            try {
                val today = DateTimeUtils.today()
                val yesterday = DateTimeUtils.daysAgo(1)

                // Get all plans from last 7 days to find recurring tasks
                val weekAgo = DateTimeUtils.daysAgo(7)
                val recentPlans = planRepository.getPlansInRange(weekAgo, yesterday)

                for (plan in recentPlans) {
                    val tasks = planRepository.getPlanTasks(plan.planId)
                    for (task in tasks) {
                        if (task.repeatRule != null && task.status != TaskStatus.SKIPPED) {
                            // Check if this recurring task already has an instance today
                            val todayPlans = planRepository.getPlansInRange(today, today)
                            var alreadyExists = false
                            for (tp in todayPlans) {
                                val todayTasks = planRepository.getPlanTasks(tp.planId)
                                if (todayTasks.any { it.name == task.name && it.repeatRule == task.repeatRule }) {
                                    alreadyExists = true
                                    break
                                }
                            }
                            if (!alreadyExists) {
                                addManualTask(
                                    task.copy(taskId = 0, planId = 0, status = TaskStatus.PENDING, earnedExp = 0, completedAt = null),
                                    today
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to generate recurring tasks", e)
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
