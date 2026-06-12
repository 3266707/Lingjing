package com.lingjing.feature.plan.history

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lingjing.core.common.constant.NavRoutes
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.domain.model.*
import com.lingjing.domain.repository.PlanRepository
import com.lingjing.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ──────────────────────────────────────────────
//  Domain data for the grouped UI
// ──────────────────────────────────────────────

/** A single section of plans grouped under one heading. */
data class PlanDateGroup(
    val label: String,          // "今天", "昨天", or a formatted date
    val date: String,           // yyyy-MM-dd
    val plans: List<Plan>
)

/** History filter type */
enum class HistoryFilter(val displayName: String) {
    ALL("全部"),
    DAILY("每日"),
    LONG_TERM("长期")
}

// ──────────────────────────────────────────────
//  ViewModel
// ──────────────────────────────────────────────

data class PlanHistoryUiState(
    val groups: List<PlanDateGroup> = emptyList(),
    val allPlans: List<Plan> = emptyList(),
    val selectedFilter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class PlanHistoryViewModel @Inject constructor(
    private val planRepository: PlanRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PlanHistoryVM"
    }

    private val _state = MutableStateFlow(PlanHistoryUiState())
    val state: StateFlow<PlanHistoryUiState> = _state.asStateFlow()

    init {
        loadPlans()
    }

    fun loadPlans() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                planRepository.getAllPlans().collect { plans ->
                    val filtered = filterPlans(plans, _state.value.selectedFilter)
                    val groups = groupPlansByDate(filtered)
                    _state.update {
                        it.copy(
                            allPlans = plans,
                            groups = groups,
                            isLoading = false,
                            error = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plan history", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "加载失败: ${e.message?.take(100) ?: "未知错误"}"
                    )
                }
            }
        }
    }

    fun selectFilter(filter: HistoryFilter) {
        if (_state.value.selectedFilter == filter) return
        _state.update { it.copy(selectedFilter = filter) }
        // Re-filter the existing plan list
        val filtered = filterPlans(_state.value.allPlans, filter)
        val groups = groupPlansByDate(filtered)
        _state.update { it.copy(groups = groups) }
    }

    private fun filterPlans(plans: List<Plan>, filter: HistoryFilter): List<Plan> {
        return when (filter) {
            HistoryFilter.ALL -> plans
            HistoryFilter.DAILY -> plans.filter { it.type == PlanType.DAILY || it.type == PlanType.REPEAT }
            HistoryFilter.LONG_TERM -> plans.filter { it.type == PlanType.LONG_TERM }
        }
    }

    private fun groupPlansByDate(plans: List<Plan>): List<PlanDateGroup> {
        if (plans.isEmpty()) return emptyList()

        val today = DateTimeUtils.today()
        val yesterday = DateTimeUtils.daysAgo(1)
        val groups = mutableListOf<PlanDateGroup>()

        // 按日期分组（从新到旧）
        val groupedByDate = plans.groupBy { it.date }
            .entries
            .sortedByDescending { it.key }

        for ((date, datePlans) in groupedByDate) {
            val label = when (date) {
                today -> "今天"
                yesterday -> "昨天"
                else -> DateTimeUtils.formatChinese(date)
            }
            groups.add(PlanDateGroup(label = label, date = date, plans = datePlans))
        }

        return groups
    }
}

// ──────────────────────────────────────────────
//  Composable Screen
// ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanHistoryScreen(
    navController: NavController,
    viewModel: PlanHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("计划历史", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ── 过滤芯片 ──
            FilterChipRow(
                selectedFilter = state.selectedFilter,
                onFilterSelected = { viewModel.selectFilter(it) }
            )

            // ── 内容区域 ──
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = FlowerCyan)
                    }
                }

                state.error != null -> {
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
                            onClick = { viewModel.loadPlans() },
                            colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
                        ) {
                            Text("重试")
                        }
                    }
                }

                state.groups.isEmpty() -> {
                    EmptyHistoryState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        state.groups.forEach { group ->
                            // 分组标题
                            item(key = "header_${group.date}") {
                                DateGroupHeader(
                                    label = group.label,
                                    date = group.date,
                                    planCount = group.plans.size
                                )
                            }

                            // 该分组下的计划卡片
                            items(
                                items = group.plans,
                                key = { "plan_${it.planId}" }
                            ) { plan ->
                                PlanHistoryCard(
                                    plan = plan,
                                    onClick = {
                                        navController.navigate(NavRoutes.planDetail(plan.planId))
                                    }
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
//  Filter Chips
// ──────────────────────────────────────────────

@Composable
private fun FilterChipRow(
    selectedFilter: HistoryFilter,
    onFilterSelected: (HistoryFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HistoryFilter.entries.forEach { filter ->
            val isSelected = filter == selectedFilter
            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FlowerCyan.copy(alpha = 0.15f),
                    selectedLabelColor = FlowerCyan
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) FlowerCyan.copy(alpha = 0.5f) else Ochre.copy(alpha = 0.3f),
                    selectedBorderColor = FlowerCyan,
                    enabled = true,
                    selected = isSelected
                )
            )
        }
    }
}

// ──────────────────────────────────────────────
//  Date Group Header
// ──────────────────────────────────────────────

@Composable
private fun DateGroupHeader(
    label: String,
    date: String,
    planCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = InkBlack
        )
        Spacer(Modifier.width(8.dp))
        // 日期（非今天/昨天时已包含在 label 中）
        if (label == "今天" || label == "昨天") {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = LightInk
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "${planCount}个计划",
            style = MaterialTheme.typography.labelSmall,
            color = LightInk
        )
    }
}

// ──────────────────────────────────────────────
//  Plan History Card
// ──────────────────────────────────────────────

@Composable
private fun PlanHistoryCard(
    plan: Plan,
    onClick: () -> Unit
) {
    val typeLabel = when (plan.type) {
        PlanType.DAILY -> "每日"
        PlanType.LONG_TERM -> "长期"
        PlanType.REPEAT -> "重复"
    }
    val typeColor = when (plan.type) {
        PlanType.DAILY -> FlowerCyan
        PlanType.LONG_TERM -> StoneGreen
        PlanType.REPEAT -> Gamboge
    }

    val completedCount = plan.completedTasks
    val totalCount = plan.totalTasks
    val completionRate = plan.completionRate

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 类型标签
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = typeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = typeLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 完成率
                val ratePercent = (completionRate * 100).toInt()
                val rateColor = when {
                    completionRate >= 1f -> SuccessGreen
                    completionRate >= 0.5f -> FlowerCyan
                    else -> LightInk
                }
                Text(
                    text = "${ratePercent}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = rateColor
                )
            }

            Spacer(Modifier.height(8.dp))

            // 标题
            Text(
                text = plan.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = InkBlack
            )

            // 来源（如果有）
            if (plan.sourceText.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = plan.sourceText,
                    style = MaterialTheme.typography.bodySmall,
                    color = LightInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(8.dp))

            // 进度条 + 统计
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "任务 $completedCount/$totalCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightInk
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { completionRate },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .let { mod ->
                            // RoundedCornerShape applied via drawing
                            mod
                        },
                    color = when {
                        completionRate >= 1f -> SuccessGreen
                        completionRate >= 0.5f -> FlowerCyan
                        else -> StoneGreen
                    },
                    trackColor = FlowerCyan.copy(alpha = 0.1f)
                )
            }
        }
    }
}

// ──────────────────────────────────────────────
//  Empty State
// ──────────────────────────────────────────────

@Composable
private fun EmptyHistoryState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Filled.History,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = LightInk.copy(alpha = 0.4f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "暂无计划记录",
                style = MaterialTheme.typography.titleMedium,
                color = LightInk,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "完成今日看板中的计划生成\n将自动记录在此处",
                style = MaterialTheme.typography.bodyMedium,
                color = LightInk.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}
