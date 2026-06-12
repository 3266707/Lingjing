package com.lingjing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.domain.model.PlanTask
import com.lingjing.domain.model.Priority
import com.lingjing.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onSave: (PlanTask, date: String) -> Unit
) {
    var taskName by remember { mutableStateOf("") }
    var selectedAttribute by remember { mutableStateOf("wisdom") }
    var estimatedMinutes by remember { mutableStateOf("30") }
    var difficultyLevel by remember { mutableStateOf(3) }
    var selectedPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var isRestTask by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(DateTimeUtils.today()) }
    var selectedRepeat by remember { mutableStateOf("none") }
    var showDatePicker by remember { mutableStateOf(false) }

    val attributeOptions = listOf(
        "wisdom" to "悟性📚", "physique" to "体魄💪", "perception" to "神识🧘",
        "energy" to "精力⚡", "will" to "意志🔥"
    )
    val attributeColors = mapOf(
        "wisdom" to AttributeColors.Wisdom, "physique" to AttributeColors.Physique,
        "perception" to AttributeColors.Perception, "energy" to AttributeColors.Energy,
        "will" to AttributeColors.Will
    )

    // Date picker dialog
    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                java.time.LocalDate.parse(selectedDate).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) { System.currentTimeMillis() }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        val instant = java.time.Instant.ofEpochMilli(millis)
                        val local = instant.atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                        selectedDate = local.toString()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = dateState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加修炼事项", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Task name
                OutlinedTextField(
                    value = taskName,
                    onValueChange = { taskName = it; nameError = false },
                    label = { Text("事项名称") },
                    isError = nameError,
                    supportingText = if (nameError) {{ Text("名称不能为空") }} else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date + Repeat row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date button
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("📅 $selectedDate", fontSize = 12.sp, maxLines = 1)
                    }

                    // Repeat selector
                    var repeatExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { repeatExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val label = when (selectedRepeat) {
                                "daily" -> "🔄 每日"; "weekly" -> "📅 每周"
                                "monthly" -> "📆 每月"; else -> "➖ 不重复"
                            }
                            Text(label, fontSize = 12.sp)
                        }
                        DropdownMenu(
                            expanded = repeatExpanded,
                            onDismissRequest = { repeatExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("不重复") }, onClick = { selectedRepeat = "none"; repeatExpanded = false })
                            DropdownMenuItem(text = { Text("🔄 每日") }, onClick = { selectedRepeat = "daily"; repeatExpanded = false })
                            DropdownMenuItem(text = { Text("📅 每周") }, onClick = { selectedRepeat = "weekly"; repeatExpanded = false })
                            DropdownMenuItem(text = { Text("📆 每月") }, onClick = { selectedRepeat = "monthly"; repeatExpanded = false })
                        }
                    }
                }

                // Attribute
                Text("属性", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    attributeOptions.forEach { (key, label) ->
                        FilterChip(
                            selected = selectedAttribute == key,
                            onClick = { selectedAttribute = key },
                            label = { Text(label, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = (attributeColors[key] ?: FlowerCyan).copy(alpha = 0.15f)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Time + Difficulty row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = estimatedMinutes,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) estimatedMinutes = it },
                        label = { Text("时长(分)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Difficulty
                Text("难度", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val dl = mapOf(1 to "轻松", 2 to "简单", 3 to "普通", 4 to "困难", 5 to "地狱")
                    (1..5).forEach { lv ->
                        FilterChip(
                            selected = difficultyLevel == lv,
                            onClick = { difficultyLevel = lv },
                            label = { Text(dl[lv] ?: "", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Gamboge.copy(alpha = 0.2f)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Priority
                Text("优先级", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val pl = mapOf(Priority.HIGH to "高", Priority.MEDIUM to "中", Priority.LOW to "低")
                    val pc = mapOf(Priority.HIGH to DawnRed.copy(alpha=0.15f), Priority.MEDIUM to Gamboge.copy(alpha=0.2f), Priority.LOW to SuccessGreen.copy(alpha=0.15f))
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = selectedPriority == p,
                            onClick = { selectedPriority = p },
                            label = { Text(pl[p] ?: "") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = pc[p] ?: MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }
                }

                // Rest toggle
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text("休息任务", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isRestTask, onCheckedChange = { isRestTask = it }, colors = SwitchDefaults.colors(checkedTrackColor = FlowerCyan))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = taskName.trim()
                    if (name.isBlank()) { nameError = true; return@Button }
                    val task = PlanTask(
                        name = name,
                        attributeKey = selectedAttribute,
                        estimatedMinutes = (estimatedMinutes.toIntOrNull() ?: 30).coerceIn(1, 999),
                        difficultyScore = difficultyLevel,
                        priority = selectedPriority,
                        isRest = isRestTask,
                        baseExp = difficultyLevel * 10,
                        orderIndex = 0,
                        repeatRule = if (selectedRepeat == "none") null else selectedRepeat
                    )
                    onSave(task, selectedDate)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
