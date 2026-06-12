package com.lingjing.feature.plan.edit

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lingjing.core.common.constant.DbConstants
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

data class PlanEditUiState(
    val plan: Plan? = null,
    val tasks: List<PlanTask> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val taskToDeleteIndex: Int = -1
)

@HiltViewModel
class PlanEditViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PlanEditVM"
    }

    private val _state = MutableStateFlow(PlanEditUiState())
    val state: StateFlow<PlanEditUiState> = _state.asStateFlow()

    private var loadedPlanId: Long = 0L
    private var nextTempId = -1L

    fun loadPlan(planId: Long) {
        if (planId <= 0L) return
        loadedPlanId = planId

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val plan = planRepository.getPlanWithTasks(planId)
                if (plan == null) {
                    _state.update { it.copy(isLoading = false, error = "计划未找到") }
                    return@launch
                }
                // 初始化下一临时ID
                val maxId = plan.tasks.maxOfOrNull { it.taskId } ?: 0L
                nextTempId = -maxId - 1
                if (nextTempId >= 0) nextTempId = -1L

                _state.update {
                    it.copy(
                        isLoading = false,
                        plan = plan,
                        tasks = plan.tasks.sortedBy { t -> t.orderIndex }
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plan $planId", e)
                _state.update {
                    it.copy(isLoading = false, error = "加载失败: ${e.message?.take(100)}")
                }
            }
        }
    }

    fun updateTaskName(index: Int, name: String) {
        val tasks = _state.value.tasks.toMutableList()
        if (index in tasks.indices) {
            tasks[index] = tasks[index].copy(name = name)
            _state.update { it.copy(tasks = tasks) }
        }
    }

    fun updateTaskAttribute(index: Int, attributeKey: String) {
        val tasks = _state.value.tasks.toMutableList()
        if (index in tasks.indices) {
            tasks[index] = tasks[index].copy(attributeKey = attributeKey)
            _state.update { it.copy(tasks = tasks) }
        }
    }

    fun updateTaskMinutes(index: Int, minutes: Int) {
        val tasks = _state.value.tasks.toMutableList()
        if (index in tasks.indices) {
            tasks[index] = tasks[index].copy(estimatedMinutes = minutes.coerceIn(1, 480))
            _state.update { it.copy(tasks = tasks) }
        }
    }

    fun updateTaskDifficulty(index: Int, difficulty: Int) {
        val tasks = _state.value.tasks.toMutableList()
        if (index in tasks.indices) {
            tasks[index] = tasks[index].copy(difficultyScore = difficulty.coerceIn(1, 5))
            _state.update { it.copy(tasks = tasks) }
        }
    }

    fun moveTaskUp(index: Int) {
        if (index <= 0) return
        val tasks = _state.value.tasks.toMutableList()
        if (index >= tasks.size) return
        val temp = tasks[index]
        tasks[index] = tasks[index - 1]
        tasks[index - 1] = temp
        // 更新 orderIndex
        _state.update { it.copy(tasks = tasks.mapIndexed { i, t -> t.copy(orderIndex = i) }) }
    }

    fun moveTaskDown(index: Int) {
        val tasks = _state.value.tasks.toMutableList()
        if (index < 0 || index >= tasks.size - 1) return
        val temp = tasks[index]
        tasks[index] = tasks[index + 1]
        tasks[index + 1] = temp
        _state.update { it.copy(tasks = tasks.mapIndexed { i, t -> t.copy(orderIndex = i) }) }
    }

    fun requestDeleteTask(index: Int) {
        _state.update { it.copy(showDeleteDialog = true, taskToDeleteIndex = index) }
    }

    fun confirmDeleteTask() {
        val index = _state.value.taskToDeleteIndex
        if (index < 0) return
        val tasks = _state.value.tasks.toMutableList()
        if (index in tasks.indices) {
            tasks.removeAt(index)
            _state.update {
                it.copy(
                    tasks = tasks.mapIndexed { i, t -> t.copy(orderIndex = i) },
                    showDeleteDialog = false,
                    taskToDeleteIndex = -1
                )
            }
        } else {
            _state.update { it.copy(showDeleteDialog = false, taskToDeleteIndex = -1) }
        }
    }

    fun cancelDeleteTask() {
        _state.update { it.copy(showDeleteDialog = false, taskToDeleteIndex = -1) }
    }

    fun addNewTask() {
        val tasks = _state.value.tasks.toMutableList()
        val newIndex = tasks.size
        val newTask = PlanTask(
            taskId = nextTempId,
            planId = loadedPlanId,
            name = "",
            attributeKey = "wisdom",
            estimatedMinutes = 30,
            difficultyScore = 3,
            priority = Priority.MEDIUM,
            orderIndex = newIndex,
            status = TaskStatus.PENDING,
            baseExp = 20,
            earnedExp = 0
        )
        nextTempId--
        tasks.add(newTask)
        _state.update { it.copy(tasks = tasks) }
    }

    fun saveAll() {
        val plan = _state.value.plan ?: return
        val tasks = _state.value.tasks
        if (tasks.any { it.name.isBlank() }) {
            _state.update { it.copy(error = "请填写所有任务的名称") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            try {
                // 更新计划
                planRepository.updatePlan(plan)

                // 删除已移除的任务（原计划中有但编辑列表中没有的）
                val originalTaskIds = plan.tasks.map { it.taskId }.toSet()
                val currentTaskIds = tasks.map { it.taskId }.toSet()
                val removedIds = originalTaskIds - currentTaskIds
                removedIds.forEach { removedId ->
                    if (removedId > 0) {
                        planRepository.deletePlanTask(removedId)
                    }
                }

                // 更新或插入任务
                tasks.forEach { task ->
                    if (task.taskId <= 0L) {
                        // 新任务（临时ID），插入
                        planRepository.insertPlanTask(
                            task.copy(taskId = 0L, planId = loadedPlanId)
                        )
                    } else {
                        planRepository.updatePlanTask(task)
                    }
                }

                _state.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save", e)
                _state.update {
                    it.copy(isSaving = false, error = "保存失败: ${e.message?.take(100)}")
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun resetSaveSuccess() {
        _state.update { it.copy(saveSuccess = false) }
    }
}

// ──────────────────────────────────────────────
//  Composable Screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditScreen(
    navController: NavController,
    planId: Long? = null,
    viewModel: PlanEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(planId) {
        planId?.let { viewModel.loadPlan(it) }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar(message = "保存成功", duration = SnackbarDuration.Short)
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(message = msg, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    // 删除确认对话框
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteTask() },
            title = { Text("确认删除") },
            text = { Text("确定要删除该任务吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteTask() },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteTask() }) {
                    Text("取消")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("编辑计划", fontWeight = FontWeight.Bold) },
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

                state.plan == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error ?: "计划未找到",
                            style = MaterialTheme.typography.bodyLarge,
                            color = LightInk
                        )
                    }
                }

                else -> {
                    val tasks = state.tasks

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // ── 计划信息 ──
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = state.plan!!.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "共 ${tasks.size} 项任务",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = LightInk
                                    )
                                }
                            }
                        }

                        // ── 任务编辑区标题 ──
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "任务列表",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                FilledTonalButton(
                                    onClick = { viewModel.addNewTask() },
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = FlowerCyan.copy(alpha = 0.15f),
                                        contentColor = FlowerCyan
                                    )
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("添加任务")
                                }
                            }
                        }

                        // ── 空任务状态 ──
                        if (tasks.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "点击上方「添加任务」按钮添加任务",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LightInk
                                    )
                                }
                            }
                        }

                        // ── 每个任务编辑卡片 ──
                        itemsIndexed(tasks, key = { _, t -> t.taskId }) { index, task ->
                            EditTaskCard(
                                index = index,
                                task = task,
                                isFirst = index == 0,
                                isLast = index == tasks.lastIndex,
                                onNameChange = { viewModel.updateTaskName(index, it) },
                                onAttributeChange = { viewModel.updateTaskAttribute(index, it) },
                                onMinutesChange = { viewModel.updateTaskMinutes(index, it) },
                                onDifficultyChange = { viewModel.updateTaskDifficulty(index, it) },
                                onMoveUp = { viewModel.moveTaskUp(index) },
                                onMoveDown = { viewModel.moveTaskDown(index) },
                                onDelete = { viewModel.requestDeleteTask(index) }
                            )
                        }

                        // ── 保存按钮 ──
                        item {
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.saveAll() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                enabled = !state.isSaving,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = PaperWhite,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(
                                    text = "保存所有更改",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Edit Task Card
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditTaskCard(
    index: Int,
    task: PlanTask,
    isFirst: Boolean,
    isLast: Boolean,
    onNameChange: (String) -> Unit,
    onAttributeChange: (String) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onDifficultyChange: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit
) {
    val attrColor = getAttrColor(task.attributeKey)
    var minutesText by remember(task.taskId) {
        mutableStateOf(task.estimatedMinutes.toString())
    }
    var attributeExpanded by remember { mutableStateOf(false) }

    // 属性选项
    val attributeOptions = listOf(
        "wisdom" to "悟性" to getAttrEmoji("wisdom"),
        "physique" to "体魄" to getAttrEmoji("physique"),
        "perception" to "感知" to getAttrEmoji("perception"),
        "energy" to "精力" to getAttrEmoji("energy"),
        "will" to "意志" to getAttrEmoji("will")
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 序号 + 移动/删除按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = attrColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "T${index + 1}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = attrColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.weight(1f))

                // 上移
                IconButton(
                    onClick = onMoveUp,
                    enabled = !isFirst,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "上移",
                        tint = if (isFirst) LightInk.copy(alpha = 0.3f) else FlowerCyan
                    )
                }

                // 下移
                IconButton(
                    onClick = onMoveDown,
                    enabled = !isLast,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "下移",
                        tint = if (isLast) LightInk.copy(alpha = 0.3f) else FlowerCyan
                    )
                }

                // 删除
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "删除",
                        tint = ErrorRed.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── 任务名称 ──
            OutlinedTextField(
                value = task.name,
                onValueChange = onNameChange,
                label = { Text("任务名称") },
                placeholder = { Text("输入任务名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = attrColor,
                    cursorColor = attrColor,
                    focusedLabelColor = attrColor
                )
            )

            Spacer(Modifier.height(12.dp))

            // ── 属性 + 分钟 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 属性下拉
                Box(modifier = Modifier.weight(1f)) {
                    ExposedDropdownMenuBox(
                        expanded = attributeExpanded,
                        onExpandedChange = { attributeExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = attributeOptions.first { it.first.first == task.attributeKey }.let {
                                "${it.second} ${it.first.second}"
                            },
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("属性") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = attributeExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = attrColor,
                                focusedLabelColor = attrColor
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = attributeExpanded,
                            onDismissRequest = { attributeExpanded = false }
                        ) {
                            attributeOptions.forEach { (keyAndName, emoji) ->
                                val (key, name) = keyAndName
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(emoji, fontSize = MaterialTheme.typography.bodyLarge.fontSize)
                                            Spacer(Modifier.width(8.dp))
                                            Text(name)
                                        }
                                    },
                                    onClick = {
                                        onAttributeChange(key)
                                        attributeExpanded = false
                                    },
                                    leadingIcon = {
                                        if (key == task.attributeKey) {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = FlowerCyan,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // 估计分钟
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { input ->
                        minutesText = input
                        val parsed = input.filter { it.isDigit() }.take(4).toIntOrNull()
                        if (parsed != null) {
                            onMinutesChange(parsed)
                        }
                    },
                    label = { Text("分钟") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = attrColor,
                        focusedLabelColor = attrColor
                    )
                )
            }

            Spacer(Modifier.height(12.dp))

            // ── 难度滑块 ──
            Text(
                text = "难度: ${task.difficultyScore}/5",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = LightInk
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("简单", style = MaterialTheme.typography.labelSmall, color = LightInk)
                Slider(
                    value = task.difficultyScore.toFloat(),
                    onValueChange = { onDifficultyChange(it.toInt()) },
                    valueRange = 1f..5f,
                    steps = 3,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = attrColor,
                        activeTrackColor = attrColor
                    )
                )
                Text("极难", style = MaterialTheme.typography.labelSmall, color = LightInk)
            }

            // 难度文字描述
            val difficultyLabel = when (task.difficultyScore) {
                1 -> "极简 · 轻松完成"
                2 -> "简单 · 稍有挑战"
                3 -> "适中 · 正常水平"
                4 -> "困难 · 需要专注"
                5 -> "极难 · 全力突破"
                else -> "适中"
            }
            Text(
                text = difficultyLabel,
                style = MaterialTheme.typography.labelSmall,
                color = LightInk.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}
