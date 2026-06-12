package com.lingjing.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.lingjing.core.common.constant.NavRoutes
import com.lingjing.domain.model.*
import com.lingjing.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayBoard(
    navController: NavController,
    viewModel: TodayBoardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Undo Snackbar
    LaunchedEffect(state.lastCompletedTaskId) {
        if (state.lastCompletedTaskId != null) {
            val result = snackbarHostState.showSnackbar(
                message = "经验+${state.lastCompletedExp}",
                actionLabel = "撤销",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.revertLastComplete()
            } else {
                viewModel.dismissUndo()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 顶部状态栏
            TopStatusBar(
                realm = state.realm,
                qiConcentration = state.qiConcentration,
                energy = state.energy,
                maxEnergy = state.maxEnergy
            )

            // 错误提示
            if (state.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DawnRed.copy(alpha = 0.1f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = DawnRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.error!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = DawnRed,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("关闭", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // 完成率统计条
            if (state.todayTasks.isNotEmpty()) {
                CompletionStatsBar(
                    completed = state.completedTasks,
                    total = state.totalTasks,
                    rate = state.completionRate
                )
            }

            // 内容区域（loading / 空状态 / 任务列表）
            Box(modifier = Modifier.weight(1f)) {
                if (state.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FlowerCyan)
                    }
                } else if (state.todayTasks.isEmpty() && state.error == null) {
                    EmptyState(onGenerate = { navController.navigate(NavRoutes.PLAN_GENERATE) })
                } else if (state.todayTasks.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.todayTasks, key = { it.taskId }) { task ->
                            TaskCard(
                                task = task,
                                onComplete = { viewModel.completeTask(task.taskId) },
                                onSkip = { viewModel.skipTask(task.taskId) },
                                onUndo = { viewModel.undoSpecificTask(task.taskId) }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }

            // 底部操作栏
            BottomActionBar(
                onGenerate = { navController.navigate(NavRoutes.PLAN_GENERATE) },
                onAddManual = { showAddDialog = true },
                onReview = { navController.navigate(NavRoutes.REVIEW_WRITE) }
            )

            // 手动添加任务对话框
            if (showAddDialog) {
                AddTaskDialog(
                    onDismiss = { showAddDialog = false },
                    onSave = { task, date ->
                        viewModel.addManualTask(task, date)
                        showAddDialog = false
                    }
                )
            }
        }
    }
}

@Composable
fun TopStatusBar(realm: Realm, qiConcentration: Int, energy: Int, maxEnergy: Int) {
    val realmColor = when (realm) {
        Realm.QI_REFINING -> RealmColors.QiRefining
        Realm.FOUNDATION -> RealmColors.Foundation
        Realm.GOLDEN_CORE -> RealmColors.GoldenCore
        Realm.NASCENT_SOUL -> RealmColors.NascentSoul
        Realm.SPIRIT_TRANSFORMATION -> RealmColors.SpiritTransformation
    }
    val dateStr = com.lingjing.core.common.util.DateTimeUtils.today()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 第一行：境界 + 日期
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = realm.displayName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = realmColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (realm) {
                            Realm.QI_REFINING -> "初入道途，未来可期"
                            Realm.FOUNDATION -> "道基初成，渐入佳境"
                            Realm.GOLDEN_CORE -> "丹成九转，金光护体"
                            Realm.NASCENT_SOUL -> "元胎化婴，神通初显"
                            Realm.SPIRIT_TRANSFORMATION -> "天人合一，道法自然"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "📅 $dateStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = LightInk
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "☁️ 灵气 ${qiConcentration}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (qiConcentration >= 20) SuccessGreen else QiConcentrationColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // 真元条
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ 真元",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Text(
                        text = "$energy / $maxEnergy",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (energy < maxEnergy * 0.2) DawnRed else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (energy.toFloat() / maxEnergy).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(EnergyBarShape),
                    color = if (energy < maxEnergy * 0.2) DawnRed else Gamboge,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                if (energy < maxEnergy * 0.2) {
                    Spacer(Modifier.height(4.dp))
                    Text("⚠ 气海枯竭，后续任务经验减半", style = MaterialTheme.typography.labelSmall, color = DawnRed)
                }
            }
        }
    }
}

@Composable
fun CompletionStatsBar(completed: Int, total: Int, rate: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("📋 今日进度", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$completed / $total  (${"%.0f".format(rate * 100)}%)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = when {
                    rate >= 0.8f -> SuccessGreen
                    rate >= 0.5f -> Gamboge
                    else -> DawnRed
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskCard(
    task: PlanTask,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onUndo: () -> Unit
) {
    val attrColor = when (task.attributeKey) {
        "wisdom" -> Color(0xFF7C4DFF)
        "physique" -> Color(0xFFFF6B6B)
        "perception" -> Color(0xFF4ECDC4)
        "energy" -> Color(0xFFFFD93D)
        "will" -> Color(0xFFFF8C42)
        else -> FlowerCyan
    }
    val attrEmoji = when (task.attributeKey) {
        "wisdom" -> "📚"; "physique" -> "💪"; "perception" -> "🧘"
        "energy" -> "⚡"; "will" -> "🔥"; else -> "📋"
    }

    val isDone = task.status == TaskStatus.DONE
    val bgColor by animateColorAsState(
        if (isDone) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        else MaterialTheme.colorScheme.surface
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isDone)
                    Modifier.combinedClickable(onClick = {}, onLongClick = onUndo)
                else
                    Modifier.clickable { onComplete() }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDone) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // LifeUp-style colored completion circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isDone) attrColor else attrColor.copy(alpha = 0.12f),
                        CircleShape
                    )
                    .then(
                        if (!isDone)
                            Modifier.clickable { onComplete() }
                        else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isDone) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "已完成",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                } else {
                    Text(attrEmoji, fontSize = 18.sp)
                }
            }

            Spacer(Modifier.width(14.dp))

            // Task info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isDone) FontWeight.Normal else FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDone) LightInk else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⏱${task.estimatedMinutes}min", style = MaterialTheme.typography.labelSmall, color = LightInk)
                    Text("✨+${task.earnedExp.ifZero(task.baseExp)}", style = MaterialTheme.typography.labelSmall, color = Gamboge)
                }
            }

            // Right side: XP earned or skip
            if (isDone) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("+${task.earnedExp}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = attrColor)
                    Text("经验", style = MaterialTheme.typography.labelSmall, color = LightInk)
                }
            } else {
                IconButton(
                    onClick = onSkip,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "跳过", tint = LightInk.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun Int.ifZero(fallback: Int) = if (this == 0) fallback else this

@Composable
fun EmptyState(onGenerate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏔️", fontSize = 48.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "今日尚无修炼计划",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "点击下方按钮，让 AI 为你生成计划",
            style = MaterialTheme.typography.bodySmall,
            color = LightInk
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onGenerate,
            colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("生成今日计划")
        }
    }
}

@Composable
fun BottomActionBar(onGenerate: () -> Unit, onAddManual: () -> Unit, onReview: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onReview) {
                Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text("写札记")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onGenerate, shape = RoundedCornerShape(20.dp)) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI生成")
                }
                Button(
                    onClick = onAddManual,
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("添加")
                }
            }
        }
    }
}
