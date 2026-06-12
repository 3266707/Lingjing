package com.lingjing.feature.settings

import android.content.SharedPreferences
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lingjing.BuildConfig
import com.lingjing.core.common.constant.DbConstants
import com.lingjing.data.remote.api.CustomPersona
import com.lingjing.data.remote.api.PlanApiService
import com.lingjing.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val planApiService: PlanApiService,
    private val securePrefs: SharedPreferences
) : ViewModel() {

    companion object {
        private const val PREF_THEME_MODE = "theme_mode"
    }

    data class UiState(
        val apiKey: String = "",
        val hasApiKey: Boolean = false,
        val showApiKey: Boolean = false,
        val selectedPersona: String = DbConstants.PERSONA_RATIONAL,
        val isTestingApi: Boolean = false,
        val testResult: String? = null,
        val themeMode: String = "system", // "system", "light", "dark"
        val personaAutoSwitch: Boolean = false,
        val customPersonas: List<CustomPersona> = emptyList(),
        val showCustomPersonaDialog: Boolean = false,
        val selectedProvider: String = "deepseek",
        val baseUrl: String = "",
        val modelName: String = "deepseek-chat"
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        val existingKey = planApiService.getApiKey()
        val savedTheme = securePrefs.getString(PREF_THEME_MODE, "system") ?: "system"
        val savedProvider = planApiService.getAiProvider()
        val savedBaseUrl = planApiService.getAiBaseUrl()
        val savedModel = planApiService.getAiModel()
        val defaults = PlanApiService.PROVIDER_DEFAULTS[savedProvider]
            ?: PlanApiService.PROVIDER_DEFAULTS["deepseek"]!!
        _state.update {
            it.copy(
                hasApiKey = !existingKey.isNullOrBlank(),
                apiKey = existingKey ?: "",
                selectedPersona = planApiService.getActivePersona(),
                themeMode = savedTheme,
                personaAutoSwitch = planApiService.getAutoSwitchEnabled(),
                customPersonas = planApiService.getCustomPersonas(),
                selectedProvider = savedProvider,
                baseUrl = savedBaseUrl ?: defaults.baseUrl,
                modelName = savedModel ?: defaults.model
            )
        }
    }

    fun selectTheme(mode: String) {
        securePrefs.edit().putString(PREF_THEME_MODE, mode).apply()
        _state.update { it.copy(themeMode = mode) }
    }

    fun updateApiKey(key: String) {
        _state.update { it.copy(apiKey = key) }
    }

    fun saveApiKey() {
        val key = _state.value.apiKey.trim()
        if (key.isNotBlank()) {
            planApiService.setApiKey(key)
            _state.update { it.copy(hasApiKey = true) }
        }
    }

    fun clearApiKey() {
        planApiService.setApiKey("")
        _state.update { it.copy(apiKey = "", hasApiKey = false) }
    }

    fun toggleShowApiKey() {
        _state.update { it.copy(showApiKey = !it.showApiKey) }
    }

    fun selectPersona(persona: String) {
        planApiService.setPersona(persona)
        _state.update { it.copy(selectedPersona = persona) }
    }

    fun testApiConnection() {
        viewModelScope.launch {
            _state.update { it.copy(isTestingApi = true, testResult = null) }

            val result = planApiService.generateDailyPlan("测试连接")
            result.fold(
                onSuccess = {
                    _state.update { it.copy(isTestingApi = false, testResult = "✅ 连接成功！API Key 有效") }
                },
                onFailure = { e ->
                    val msg = when {
                        e.message?.contains("401") == true -> "❌ API Key 无效，请检查"
                        e.message?.contains("403") == true -> "❌ 权限不足"
                        e.message?.contains("429") == true -> "⚠️ 请求过于频繁，稍后重试"
                        e.message?.contains("timeout") == true -> "⚠️ 连接超时"
                        else -> "❌ 连接失败: ${e.message?.take(50)}"
                    }
                    _state.update { it.copy(isTestingApi = false, testResult = msg) }
                }
            )
        }
    }

    // ---- Auto Switch ----

    fun togglePersonaAutoSwitch() {
        val newValue = !_state.value.personaAutoSwitch
        planApiService.setAutoSwitchEnabled(newValue)
        _state.update { it.copy(personaAutoSwitch = newValue) }
    }

    // ---- Custom Personas ----

    private fun refreshCustomPersonas() {
        _state.update { it.copy(customPersonas = planApiService.getCustomPersonas()) }
    }

    fun showAddCustomPersonaDialog() {
        _state.update { it.copy(showCustomPersonaDialog = true) }
    }

    fun hideAddCustomPersonaDialog() {
        _state.update { it.copy(showCustomPersonaDialog = false) }
    }

    fun addCustomPersona(name: String, prompt: String) {
        planApiService.addCustomPersona(name, prompt)
        refreshCustomPersonas()
    }

    fun deleteCustomPersona(id: String) {
        planApiService.deleteCustomPersona(id)
        refreshCustomPersonas()
        // If the deleted persona was selected, state already reset in PlanApiService;
        // sync the local state
        _state.update { it.copy(selectedPersona = planApiService.getActivePersona()) }
    }

    // ---- AI Provider Config ----

    fun selectProvider(provider: String) {
        planApiService.setAiProvider(provider)
        if (provider != "custom") {
            val defaults = PlanApiService.PROVIDER_DEFAULTS[provider]
                ?: PlanApiService.PROVIDER_DEFAULTS["deepseek"]!!
            planApiService.setAiBaseUrl(defaults.baseUrl)
            planApiService.setAiModel(defaults.model)
            _state.update {
                it.copy(
                    selectedProvider = provider,
                    baseUrl = defaults.baseUrl,
                    modelName = defaults.model
                )
            }
        } else {
            // Custom: load last saved custom values, or keep current
            val savedUrl = planApiService.getAiBaseUrl()
            val savedModel = planApiService.getAiModel()
            _state.update {
                it.copy(
                    selectedProvider = provider,
                    baseUrl = savedUrl ?: "",
                    modelName = savedModel ?: ""
                )
            }
        }
    }

    fun updateBaseUrl(url: String) {
        _state.update { it.copy(baseUrl = url) }
    }

    fun updateModelName(model: String) {
        _state.update { it.copy(modelName = model) }
    }

    fun saveAiConfig() {
        val s = _state.value
        planApiService.setAiBaseUrl(s.baseUrl.trim())
        planApiService.setAiModel(s.modelName.trim())
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Custom persona add dialog
    if (state.showCustomPersonaDialog) {
        CustomPersonaDialog(
            onDismiss = { viewModel.hideAddCustomPersonaDialog() },
            onConfirm = { name, prompt ->
                viewModel.addCustomPersona(name, prompt)
                viewModel.hideAddCustomPersonaDialog()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 标题
        item {
            Text(
                text = "设置",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "配置你的修炼助手",
                style = MaterialTheme.typography.bodySmall,
                color = LightInk
            )
        }

        // === AI 服务配置（整合 API Key + 提供商 + 模型） ===
        item {
            val providerEntries = listOf("deepseek", "openai", "custom")
            var providerExpanded by remember { mutableStateOf(false) }

            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cloud, contentDescription = null, tint = StoneGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 服务配置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "选择 AI 服务商，配置 API Key 及接口参数",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightInk
                    )
                    Spacer(Modifier.height(12.dp))

                    // Provider dropdown
                    ExposedDropdownMenuBox(
                        expanded = providerExpanded,
                        onExpandedChange = { providerExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = PlanApiService.getProviderLabel(state.selectedProvider),
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text("AI 服务商") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = providerExpanded,
                            onDismissRequest = { providerExpanded = false }
                        ) {
                            providerEntries.forEach { entry ->
                                DropdownMenuItem(
                                    text = { Text(PlanApiService.getProviderLabel(entry)) },
                                    onClick = {
                                        viewModel.selectProvider(entry)
                                        providerExpanded = false
                                    },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // API Key input
                    Text(
                        "API Key",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value = state.apiKey,
                        onValueChange = { viewModel.updateApiKey(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("sk-...") },
                        visualTransformation = if (state.showApiKey)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { viewModel.toggleShowApiKey() }) {
                                Icon(
                                    if (state.showApiKey) Icons.Filled.VisibilityOff
                                    else Icons.Filled.Visibility,
                                    contentDescription = "显示/隐藏"
                                )
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // Base URL (visible for all, readonly unless custom)
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = { viewModel.updateBaseUrl(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("API Base URL") },
                        placeholder = { Text("https://api.deepseek.com/v1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        enabled = state.selectedProvider == "custom"
                    )

                    Spacer(Modifier.height(8.dp))

                    // Model name
                    OutlinedTextField(
                        value = state.modelName,
                        onValueChange = { viewModel.updateModelName(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("模型名称") },
                        placeholder = { Text("deepseek-chat / gpt-4o-mini") },
                        singleLine = true
                    )

                    Spacer(Modifier.height(12.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveApiKey()
                                viewModel.saveAiConfig()
                            },
                            enabled = state.apiKey.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = StoneGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("保存")
                        }
                        OutlinedButton(
                            onClick = { viewModel.clearApiKey() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("清除Key")
                        }
                        OutlinedButton(
                            onClick = { viewModel.testApiConnection() },
                            enabled = state.hasApiKey && !state.isTestingApi,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isTestingApi) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("测试连接")
                            }
                        }
                    }

                    // Test result
                    if (state.testResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = state.testResult!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.testResult!!.startsWith("✅")) SuccessGreen else DawnRed
                        )
                    }
                }
            }
        }

        // === AI 天道人设 ===
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = FlowerCyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 天道人设",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "选择天道意志的显现方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightInk
                    )
                    Spacer(Modifier.height(12.dp))

                    // 预设人设
                    PersonaOption(
                        key = DbConstants.PERSONA_RATIONAL,
                        title = "📊 理性引导型",
                        desc = "冷静、数据驱动，用逻辑引导成长",
                        selected = state.selectedPersona == DbConstants.PERSONA_RATIONAL,
                        onClick = { viewModel.selectPersona(DbConstants.PERSONA_RATIONAL) }
                    )
                    PersonaOption(
                        key = DbConstants.PERSONA_PASSIONATE,
                        title = "🔥 热血鼓励型",
                        desc = "元气满满、中二燃爆，用热血激励你",
                        selected = state.selectedPersona == DbConstants.PERSONA_PASSIONATE,
                        onClick = { viewModel.selectPersona(DbConstants.PERSONA_PASSIONATE) }
                    )
                    PersonaOption(
                        key = DbConstants.PERSONA_TSUNDERE,
                        title = "😤 毒舌吐槽型",
                        desc = "嘴硬心软、幽默激将，用吐槽推你前进",
                        selected = state.selectedPersona == DbConstants.PERSONA_TSUNDERE,
                        onClick = { viewModel.selectPersona(DbConstants.PERSONA_TSUNDERE) }
                    )
                    PersonaOption(
                        key = DbConstants.PERSONA_GENTLE,
                        title = "🌸 温柔治愈型",
                        desc = "温和包容、春风化雨，用温暖陪伴你",
                        selected = state.selectedPersona == DbConstants.PERSONA_GENTLE,
                        onClick = { viewModel.selectPersona(DbConstants.PERSONA_GENTLE) }
                    )

                    // 自定义人设
                    if (state.customPersonas.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = LightInk.copy(alpha = 0.2f))
                        Spacer(Modifier.height(8.dp))

                        state.customPersonas.forEach { persona ->
                            CustomPersonaOption(
                                persona = persona,
                                selected = state.selectedPersona == "custom_${persona.id}",
                                onSelect = {
                                    viewModel.selectPersona("custom_${persona.id}")
                                },
                                onDelete = {
                                    viewModel.deleteCustomPersona(persona.id)
                                }
                            )
                        }
                    }

                    // 添加自定义人设按钮
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.showAddCustomPersonaDialog() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("添加自定义人设")
                    }

                    // 自动切换人设
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = LightInk.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "自动切换人设",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "根据修炼状态自动切换天道意志",
                                style = MaterialTheme.typography.labelSmall,
                                color = LightInk
                            )
                        }
                        Switch(
                            checked = state.personaAutoSwitch,
                            onCheckedChange = { viewModel.togglePersonaAutoSwitch() },
                            colors = SwitchDefaults.colors(checkedTrackColor = FlowerCyan)
                        )
                    }
                }
            }
        }

        // === 主题 ===
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "主题设置",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ThemePreviewCard(
                            label = "☀️ 浅色",
                            bgColor = PaperWhite,
                            textColor = InkBlack,
                            isSelected = state.themeMode == "light",
                            onClick = { viewModel.selectTheme("light") }
                        )
                        ThemePreviewCard(
                            label = "🌙 深色",
                            bgColor = DarkBackground,
                            textColor = DarkText,
                            isSelected = state.themeMode == "dark",
                            onClick = { viewModel.selectTheme("dark") }
                        )
                        ThemePreviewCard(
                            label = "📱 跟随系统",
                            bgColor = MaterialTheme.colorScheme.surface,
                            textColor = MaterialTheme.colorScheme.onSurface,
                            isSelected = state.themeMode == "system",
                            onClick = { viewModel.selectTheme("system") }
                        )
                    }
                }
            }
        }

        // === 隐私 ===
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("隐私与安全", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "API Key 加密存储 · 数据仅本地保存 · AI调用仅发送必要任务信息",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightInk
                    )
                }
            }
        }

        // === 关于 ===
        item {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("关于灵境", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "版本：${BuildConfig.VERSION_NAME}  by YuRan",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightInk
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "灵境系统是一款AI驱动的日常任务管理与个人成长App。\n愿你在此间修炼，终成大道。",
                        style = MaterialTheme.typography.bodySmall,
                        color = LightInk
                    )
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
fun PersonaOption(@Suppress("UNUSED_PARAMETER") key: String, title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) FlowerCyan.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface,
        border = if (selected) ButtonDefaults.outlinedButtonBorder
        else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = FlowerCyan)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = desc,
                    style = MaterialTheme.typography.labelSmall,
                    color = LightInk
                )
            }
        }
    }
}

@Composable
fun CustomPersonaOption(
    persona: CustomPersona,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) FlowerCyan.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface,
        border = if (selected) ButtonDefaults.outlinedButtonBorder
        else null
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = FlowerCyan)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Extension,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = FlowerCyan
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = persona.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                }
                Text(
                    text = persona.prompt.take(60) + if (persona.prompt.length > 60) "..." else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = LightInk
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "删除人设",
                    tint = LightInk.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CustomPersonaDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, prompt: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "添加自定义人设",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    "定义你自己的天道意志显现方式",
                    style = MaterialTheme.typography.bodySmall,
                    color = LightInk
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("人设名称") },
                    placeholder = { Text("如：严肃师尊型") },
                    singleLine = true
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp),
                    label = { Text("系统提示词") },
                    placeholder = { Text("描述该人设的语气、风格和行为方式...") },
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), prompt.trim()) },
                enabled = name.isNotBlank() && prompt.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan)
            ) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun RowScope.ThemePreviewCard(label: String, bgColor: androidx.compose.ui.graphics.Color, textColor: androidx.compose.ui.graphics.Color, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = textColor,
                fontWeight = FontWeight.Bold
            )
            if (isSelected) {
                Spacer(Modifier.height(4.dp))
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = FlowerCyan, modifier = Modifier.size(16.dp))
            }
        }
    }
}
