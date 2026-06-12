package com.lingjing.feature.plan.detail

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.domain.model.*
import com.lingjing.domain.repository.PlanRepository
import com.lingjing.feature.attribute.dashboard.getAttrColor
import com.lingjing.feature.attribute.dashboard.getAttrEmoji
import com.lingjing.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────
//  ViewModel
// ──────────────────────────────────────────────

data class PlanDetailUiState(
    val plan: Plan? = null,
    val tasks: List<PlanTask> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val snackbarMessage: String? = null,
    val completedTaskId: Long? = null
)

@HiltViewModel
class PlanDetailViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PlanDetailVM"
    }

    private val _state = MutableStateFlow(PlanDetailUiState())
    val state: StateFlow<PlanDetailUiState> = _state.asStateFlow()

    private var loadedPlanId: Long = 0L

    fun loadPlan(planId: Long) {
        if (planId <= 0L || planId == loadedPlanId) return
        loadedPlanId = planId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            try {
                val plan = planRepository.getPlanWithTasks(planId)
                if (plan == null) {
                    _state.update {
                        it.copy(isLoading = false, error = "计划未找到")
                    }
                    return@launch
                }
                _state.update {
                    it.copy(isLoading = false, plan = plan, tasks = plan.tasks)
                }

                // 实时监听任务变化（完成后重新加载）
                planRepository.getPlanTasksFlow(planId).collect { updatedTasks ->
                    val sorted = updatedTasks.sortedBy { t -> t.orderIndex }
                    val currentPlan = _state.value.plan
                    _state.update {
                        it.copy(
                            tasks = sorted,
                            plan = currentPlan?.copy(tasks = sorted)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plan $planId", e)
                _state.update {
                    it.copy(isLoading = false, error = "加载失败: ${e.message?.take(100) ?: "未知错误"}")
                }
            }
        }
    }

    fun completeTask(task: PlanTask) {
        viewModelScope.launch {
            try {
                val completed = planRepository.completeTask(task.taskId)
                val exp = completed?.earnedExp ?: task.baseExp
                _state.update {
                    it.copy(
                        snackbarMessage = "经验+$exp",
                        completedTaskId = task.taskId
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete task", e)
                _state.update {
                    it.copy(error = "完成任务失败: ${e.message?.take(50)}")
                }
            }
        }
    }

    fun revertTask(taskId: Long) {
        viewModelScope.launch {
            try {
                planRepository.revertTask(taskId)
                _state.update {
                    it.copy(
                        snackbarMessage = "已撤销",
                        completedTaskId = null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to revert task", e)
                _state.update {
                    it.copy(error = "撤销失败: ${e.message?.take(50)}")
                }
            }
        }
    }

    fun skipTask(task: PlanTask) {
        viewModelScope.launch {
            try {
                planRepository.updateTaskStatus(task.taskId, TaskStatus.SKIPPED)
                _state.update {
                    it.copy(snackbarMessage = "已跳过任务")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to skip task", e)
                _state.update {
                    it.copy(error = "跳过任务失败: ${e.message?.take(50)}")
                }
            }
        }
    }

    fun dismissSnackbar() {
        _state.update { it.copy(snackbarMessage = null, completedTaskId = null) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}

// ──────────────────────────────────────────────
//  Composable Screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanDetailScreen(
    navController: NavController,
    planId: Long? = null,
    viewModel: PlanDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(planId) {
        planId?.let { viewModel.loadPlan(it) }
    }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            val result = snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = if (state.completedTaskId != null) "撤销" else null,
                duration = if (state.completedTaskId != null)
                    SnackbarDuration.Long
                else
                    SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed && state.completedTaskId != null) {
                viewModel.revertTask(state.completedTaskId!!)
            }
            viewModel.dismissSnackbar()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("计划详情", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlowerCyan)
                    }
                }

                state.error != null && state.plan == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "出错了",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { planId?.let { viewModel.loadPlan(it) } },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
                        ) {
                            Text("重试")
                        }
                    }
                }

                state.plan == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "计划未找到",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LightInk
                        )
                    }
                }

                else -> {
                    val plan = state.plan!!
                    val tasks = state.tasks
                    val completedCount = tasks.count {
                        it.status == TaskStatus.DONE || it.status == TaskStatus.SKIPPED
                    }
                    val totalCount = tasks.size
                    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── 计划标题与元信息 ──
                        item {
                            PlanInfoHeader(plan = plan, tasks = tasks)
                        }

                        // ── 进度条 ──
                        item {
                            ProgressSection(
                                completedCount = completedCount,
                                totalCount = totalCount,
                                progress = progress
                            )
                        }

                        // ── 任务列表标题 ──
                        item {
                            Text(
                                text = if (plan.type == PlanType.LONG_TERM) "阶段任务" else "任务列表",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        // ── 空状态 ──
                        if (tasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "暂无任务",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LightInk
                                    )
                                }
                            }
                        }

                        // ── 任务卡片 ──
                        items(tasks, key = { it.taskId }) { task ->
                            TaskCard(
                                task = task,
                                onComplete = { viewModel.completeTask(task) },
                                onSkip = { viewModel.skipTask(task) }
                            )
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Plan Info Header
// ──────────────────────────────────────────────

@Composable
private fun PlanInfoHeader(
    plan: Plan,
    tasks: List<PlanTask>
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 计划类型标签
            val typeLabel = when (plan.type) {
                PlanType.DAILY -> "每日"
                PlanType.LONG_TERM -> "长期"
                PlanType.REPEAT -> "重复"
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = FlowerCyan.copy(alpha = 0.15f)
            ) {
                Text(
                    text = typeLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = FlowerCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))

            // 标题
            Text(
                text = plan.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkBlack
            )

            // 日期
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = DateTimeUtils.formatChinese(plan.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LightInk
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "${tasks.size} 项任务",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightInk
                )
            }

            // 来源文本
            if (plan.sourceText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "来源: ${plan.sourceText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightInk,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Progress Section
// ──────────────────────────────────────────────

@Composable
private fun ProgressSection(
    completedCount: Int,
    totalCount: Int,
    progress: Float
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "完成进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "$completedCount/$totalCount (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (progress >= 1f) SuccessGreen else FlowerCyan,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .then(
                        Modifier
                            .let { mod ->
                                mod.let { m ->
                                    // rounded corner shape applied via draw behind
                                    m
                                }
                            }
                    ),
                color = if (progress >= 1f) SuccessGreen else FlowerCyan,
                trackColor = FlowerCyan.copy(alpha = 0.12f)
            )
        }
    }
}

// ──────────────────────────────────────────────
//  Task Card
// ──────────────────────────────────────────────

@Composable
private fun TaskCard(
    task: PlanTask,
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    val isDone = task.status == TaskStatus.DONE
    val isSkipped = task.status == TaskStatus.SKIPPED
    val isFinished = isDone || isSkipped

    val surfaceColor by animateColorAsState(
        targetValue = if (isDone) SuccessGreen.copy(alpha = 0.05f)
        else if (isSkipped) LightInk.copy(alpha = 0.05f)
        else MaterialTheme.colorScheme.surface,
        label = "taskCardColor"
    )

    val attrColor = getAttrColor(task.attributeKey)
    val attrEmoji = getAttrEmoji(task.attributeKey)

    val difficultyLabel = when (task.difficultyScore) {
        1 -> "极简"
        2 -> "简单"
        3 -> "适中"
        4 -> "困难"
        5 -> "极难"
        else -> "适中"
    }
    val difficultyColor = when (task.difficultyScore) {
        1 -> SuccessGreen
        2 -> FlowerCyan
        3 -> Gamboge
        4 -> WarningOrange
        5 -> ErrorRed
        else -> LightInk
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isFinished) 0.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 复选框
            if (isDone) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = "已完成",
                        tint = SuccessGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (isSkipped) {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Filled.RemoveRedEye,
                        contentDescription = "已跳过",
                        tint = LightInk,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Checkbox(
                    checked = false,
                    onCheckedChange = { onComplete() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = FlowerCyan,
                        uncheckedColor = FlowerCyan.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(Modifier.width(4.dp))

            // 任务信息
            Column(modifier = Modifier.weight(1f)) {
                // 任务名称
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (isFinished) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (isFinished) LightInk else InkBlack
                )

                Spacer(Modifier.height(4.dp))

                // 属性标签 + 难度 + 时间 + 经验
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 属性
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = attrColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$attrEmoji ${getAttrName(task.attributeKey)}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = attrColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 难度
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = difficultyColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = difficultyLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = difficultyColor,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // 估计时间
                    Text(
                        text = "${task.estimatedMinutes}分钟",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightInk
                    )

                    // 经验
                    Text(
                        text = "+${task.earnedExp}经验",
                        style = MaterialTheme.typography.labelSmall,
                        color = Gamboge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 跳过按钮（仅对未完成的任务）
            if (!isFinished) {
                TextButton(
                    onClick = onSkip,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = "跳过",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightInk
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Attribute helpers
// ──────────────────────────────────────────────

private fun getAttrName(key: String): String = when (key) {
    "wisdom" -> "悟性"
    "physique" -> "体魄"
    "perception" -> "感知"
    "energy" -> "精力"
    "will" -> "意志"
    else -> key
}
