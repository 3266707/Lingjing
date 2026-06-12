package com.lingjing.feature.plan.generation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.data.remote.api.PlanApiService
import com.lingjing.data.remote.dto.GeneratedTask
import com.lingjing.data.remote.dto.PlanGenerationResponse
import com.lingjing.domain.model.*
import com.lingjing.domain.repository.PlanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanGenerationState(
    val inputText: String = "",
    val isLoading: Boolean = false,
    val generatedPlan: PlanGenerationResponse? = null,
    val isSaving: Boolean = false,
    val savedPlanId: Long? = null,
    val error: String? = null
)

@HiltViewModel
class PlanGenerationViewModel @Inject constructor(
    private val planApiService: PlanApiService,
    private val planRepository: PlanRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlanGenerationState())
    val state: StateFlow<PlanGenerationState> = _state.asStateFlow()

    fun updateInput(text: String) {
        _state.update { it.copy(inputText = text) }
    }

    fun generatePlan() {
        val input = _state.value.inputText.trim()
        if (input.isBlank()) return

        if (!planApiService.hasApiKey()) {
            _state.update { it.copy(error = "请先在设置中配置 DeepSeek API Key") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val result = planApiService.generateDailyPlan(
                userInput = input,
                ragContext = "",
                historyContext = ""
            )

            result.fold(
                onSuccess = { plan ->
                    if (plan.tasks.isEmpty()) {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "AI生成结果为空，请尝试更详细地描述你想做的事情后重试"
                            )
                        }
                        return@launch
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            generatedPlan = plan,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "生成失败，请重试"
                        )
                    }
                }
            )
        }
    }

    fun savePlan() {
        val generated = _state.value.generatedPlan ?: return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }

            try {
                val tasks = generated.tasks.mapIndexed { index, task ->
                    PlanTask(
                        name = task.name,
                        attributeKey = task.attribute,
                        estimatedMinutes = task.estimatedMinutes,
                        difficultyScore = task.difficulty,
                        priority = Priority.MEDIUM,
                        orderIndex = index,
                        baseExp = task.baseExp,
                        isRest = task.isRest,
                        repeatRule = task.repeatRule
                    )
                }

                val plan = Plan(
                    type = PlanType.DAILY,
                    title = generated.title,
                    sourceText = _state.value.inputText,
                    date = DateTimeUtils.today(),
                    tasks = tasks
                )

                val planId = planRepository.saveGeneratedPlan(plan, tasks)
                _state.update { it.copy(isSaving = false, savedPlanId = planId) }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        error = "保存失败: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun reset() {
        _state.value = PlanGenerationState()
    }
}
