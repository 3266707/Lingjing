package com.lingjing.feature.plan.generation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.lingjing.core.common.constant.NavRoutes
import com.lingjing.ui.theme.*

@Composable
fun PlanGenerationScreen(
    navController: NavController,
    viewModel: PlanGenerationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // 监听保存完成后导航到详情页（修复竞态条件）
    LaunchedEffect(state.savedPlanId) {
        state.savedPlanId?.let { planId ->
            navController.navigate(NavRoutes.planDetail(planId)) {
                popUpTo(NavRoutes.PLAN_GENERATE) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "生成修炼计划",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "输入你今天想做的事情，AI 会帮你拆解为具体任务",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = state.inputText,
            onValueChange = { viewModel.updateInput(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = { Text("例如：今天要写完项目周报，健身40分钟，晚上冥想10分钟") },
            maxLines = 5,
            enabled = !state.isLoading
        )

        Spacer(Modifier.height(16.dp))

        // 错误提示
        if (state.error != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = DawnRed.copy(alpha = 0.1f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = DawnRed)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = DawnRed,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.clearError() }) {
                        Icon(Icons.Filled.Close, contentDescription = "关闭")
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // 生成按钮
        Button(
            onClick = { viewModel.generatePlan() },
            modifier = Modifier.fillMaxWidth(),
            enabled = state.inputText.isNotBlank() && !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("AI 正在拆解任务...")
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("生成计划")
            }
        }

        // 生成结果
        if (state.generatedPlan != null) {
            val plan = state.generatedPlan!!
            Spacer(Modifier.height(24.dp))

            Card(
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "📋 ${plan.title}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (plan.summary.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = plan.summary,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    // 任务预览
                    plan.tasks.forEachIndexed { index, task ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LightInk,
                                modifier = Modifier.width(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = task.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = buildString {
                                        append(when (task.attribute) {
                                            "wisdom" -> "📚悟性"
                                            "physique" -> "💪体魄"
                                            "perception" -> "🧘神识"
                                            "energy" -> "⚡精力"
                                            "will" -> "🔥意志"
                                            else -> "✨${task.attribute}"
                                        })
                                        append(" · ${task.estimatedMinutes}min · 难度${task.difficulty}")
                                        append(" · +${task.baseExp}EXP")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LightInk
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "☁️ 灵气浓度: ${plan.qiConcentration}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = QiConcentrationColor
                    )

                    Spacer(Modifier.height(16.dp))

                    // 保存 / 放弃
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.reset() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("重新生成")
                        }
                        Button(
                            onClick = { viewModel.savePlan() },
                            modifier = Modifier.weight(1f),
                            enabled = !state.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("保存计划")
                            }
                        }
                    }
                }
            }
        }

        // 无API Key提示
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("💡 提示", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "需要配置 DeepSeek API Key 才能使用 AI 生成功能。\n可在「设置」页面进行配置和测试连接。",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightInk
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}
