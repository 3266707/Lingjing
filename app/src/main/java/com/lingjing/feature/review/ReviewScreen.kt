package com.lingjing.feature.review

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.lingjing.core.common.util.DateTimeUtils
import com.lingjing.data.remote.api.PlanApiService
import com.lingjing.domain.model.ReviewLog
import com.lingjing.domain.repository.DailyStateRepository
import com.lingjing.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val dailyStateRepository: DailyStateRepository,
    private val planApiService: PlanApiService
) : ViewModel() {

    enum class ReviewTab { WRITE, HISTORY }

    data class UiState(
        val reviewText: String = "",
        val selectedEmotion: String? = null,
        val isAnalyzing: Boolean = false,
        val analysisResult: String? = null,
        val savedReviews: List<ReviewLog> = emptyList(),
        val isLoading: Boolean = true,
        val selectedTab: ReviewTab = ReviewTab.WRITE
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        loadReviews()
    }

    private fun loadReviews() {
        viewModelScope.launch {
            val reviews = dailyStateRepository.getAllReviews()
            _state.update { it.copy(isLoading = false, savedReviews = reviews) }
        }
    }

    fun selectTab(tab: ReviewTab) {
        _state.update { it.copy(selectedTab = tab) }
    }

    fun deleteReview(id: Long) {
        viewModelScope.launch {
            dailyStateRepository.deleteReviewLog(id)
            // Reload the list
            val reviews = dailyStateRepository.getAllReviews()
            _state.update { it.copy(savedReviews = reviews) }
        }
    }

    fun updateText(text: String) {
        _state.update { it.copy(reviewText = text) }
    }

    fun selectEmotion(emotion: String) {
        _state.update { it.copy(selectedEmotion = emotion) }
    }

    fun submitReview() {
        val text = _state.value.reviewText.trim()
        if (text.isBlank()) return

        viewModelScope.launch {
            val emotion = _state.value.selectedEmotion ?: "neutral"
            val today = DateTimeUtils.today()

            // 保存复盘记录，获取插入后的ID
            val reviewId = dailyStateRepository.insertReviewLog(
                ReviewLog(
                    date = today,
                    reviewText = text,
                    emotion = emotion
                )
            )

            // 立即刷新历史列表
            loadReviews()

            // 如果配置了API Key，调用AI分析
            if (planApiService.hasApiKey()) {
                _state.update { it.copy(isAnalyzing = true) }
                val result = planApiService.analyzeReview(
                    todaySummary = "今日修炼札记",
                    reviewText = text,
                    emotion = emotion
                )
                result.fold(
                    onSuccess = { analysis ->
                        // Auto-switch persona based on detected emotion (if enabled)
                        planApiService.autoSelectPersona(analysis.emotion)

                        // 将AI分析结果保存回数据库
                        dailyStateRepository.updateReviewLog(
                            ReviewLog(
                                id = reviewId,
                                date = today,
                                reviewText = text,
                                emotion = emotion,
                                emotionDetail = analysis.emotionDetail,
                                unfinishedReason = analysis.unfinishedReason,
                                expBalanceComment = analysis.expBalanceComment,
                                attributeBalanceComment = analysis.attributeBalanceComment,
                                difficultyShift = analysis.difficultyShift,
                                tomorrowSuggestion = analysis.tomorrowSuggestion,
                                strategyAdvice = analysis.strategyAdvice
                            )
                        )
                        // 重新加载以展示更新后的数据
                        loadReviews()

                        _state.update {
                            it.copy(
                                isAnalyzing = false,
                                reviewText = "",
                                selectedEmotion = null,
                                analysisResult = """
                                    💭 情绪：${when(analysis.emotion) {
                                        "positive" -> "正面 😊"
                                        "negative" -> "负面 😔"
                                        else -> "中性 😐"
                                    }}

                                    📝 分析：${analysis.emotionDetail}

                                    🔍 原因：${analysis.unfinishedReason}

                                    ⚖️ 经验分配：${analysis.expBalanceComment}

                                    📊 属性均衡：${analysis.attributeBalanceComment}

                                    💡 明日建议：${analysis.tomorrowSuggestion}

                                    🎯 策略：${analysis.strategyAdvice}
                                """.trimIndent()
                            )
                        }
                    },
                    onFailure = {
                        // Auto-switch based on user's selected emotion when AI analysis fails
                        planApiService.autoSelectPersona(emotion)
                        _state.update {
                            it.copy(
                                isAnalyzing = false,
                                reviewText = "",
                                selectedEmotion = null,
                                analysisResult = "✨ 札记已保存。AI分析需要配置DeepSeek API Key。"
                            )
                        }
                    }
                )
            } else {
                // Auto-switch based on user's selected emotion (fallback when no AI)
                planApiService.autoSelectPersona(emotion)
                _state.update {
                    it.copy(
                        reviewText = "",
                        selectedEmotion = null,
                        analysisResult = "✨ 修炼札记已保存。配置API Key后可获得AI深度分析。"
                    )
                }
            }
        }
    }

    fun clearAnalysis() {
        _state.update { it.copy(analysisResult = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    @Suppress("UNUSED_PARAMETER") navController: NavController,
    viewModel: ReviewViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") fullScreen: Boolean = false
) {
    val state by viewModel.state.collectAsState()

    // ---- delete confirmation state ----
    var reviewToDelete by remember { mutableStateOf<ReviewLog?>(null) }

    // ---- expanded review cards in history ----
    val expandedIds = remember { mutableStateMapOf<Long, Boolean>() }

    // ---- delete confirmation dialog ----
    if (reviewToDelete != null) {
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            title = { Text("删除札记") },
            text = { Text("确定要删除 ${reviewToDelete!!.date} 的修炼札记吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteReview(reviewToDelete!!.id)
                        reviewToDelete = null
                    }
                ) {
                    Text("删除", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ---- 标题 ----
        item {
            Text(
                text = "修炼札记",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "每日三省吾身，道心愈坚",
                style = MaterialTheme.typography.bodySmall,
                color = LightInk
            )
        }

        // ---- Tab 切换 ----
        item {
            val tabs = ReviewViewModel.ReviewTab.values()
            TabRow(
                selectedTabIndex = tabs.indexOf(state.selectedTab),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                contentColor = FlowerCyan
            ) {
                tabs.forEach { tab ->
                    val label = when (tab) {
                        ReviewViewModel.ReviewTab.WRITE -> "写札记"
                        ReviewViewModel.ReviewTab.HISTORY -> "札记历史"
                    }
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = {
                            Text(
                                label,
                                fontWeight = if (state.selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (state.selectedTab == tab) FlowerCyan else LightInk
                            )
                        }
                    )
                }
            }
        }

        // ---- Tab content ----
        when (state.selectedTab) {
            ReviewViewModel.ReviewTab.WRITE -> {
                // 输入表单
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "今日札记",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))

                            OutlinedTextField(
                                value = state.reviewText,
                                onValueChange = { viewModel.updateText(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                placeholder = { Text("今天修炼如何？有什么收获、困难或感悟...") },
                                maxLines = 8
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = "今日心境",
                                style = MaterialTheme.typography.labelMedium,
                                color = LightInk
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                EmotionChip("☺️", "正面", "positive",
                                    selected = state.selectedEmotion == "positive",
                                    onClick = { viewModel.selectEmotion("positive") }
                                )
                                EmotionChip("😐", "中性", "neutral",
                                    selected = state.selectedEmotion == "neutral",
                                    onClick = { viewModel.selectEmotion("neutral") }
                                )
                                EmotionChip("😔", "负面", "negative",
                                    selected = state.selectedEmotion == "negative",
                                    onClick = { viewModel.selectEmotion("negative") }
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.submitReview() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = state.reviewText.isNotBlank() && !state.isAnalyzing,
                                colors = ButtonDefaults.buttonColors(containerColor = FlowerCyan),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                if (state.isAnalyzing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Text(if (state.isAnalyzing) "AI 分析中..." else "提交札记")
                            }
                        }
                    }
                }

                // AI分析结果
                if (state.analysisResult != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = FlowerCyan.copy(alpha = 0.05f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "🧘 AI 修炼分析",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    TextButton(onClick = { viewModel.clearAnalysis() }) {
                                        Text("关闭")
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    state.analysisResult!!,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }

            ReviewViewModel.ReviewTab.HISTORY -> {
                if (state.savedReviews.isEmpty()) {
                    // ---- 空状态 ----
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp, bottom = 64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "📜",
                                    style = MaterialTheme.typography.displayLarge
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = "暂无修炼札记，开始写第一篇吧",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = LightInk
                                )
                            }
                        }
                    }
                } else {
                    // ---- 按周分组 ----
                    items(state.savedReviews, key = { it.id }) { review ->
                        val isExpanded = expandedIds[review.id] ?: false
                        ReviewHistoryCard(
                            review = review,
                            isExpanded = isExpanded,
                            onToggle = { expandedIds[review.id] = !isExpanded },
                            onLongClick = { reviewToDelete = review }
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// =============================================================================
// History sub-components
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReviewHistoryCard(
    review: ReviewLog,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onLongClick: () -> Unit
) {
    val emotionEmoji = when (review.emotion) {
        "positive" -> "☺️"
        "negative" -> "😔"
        else -> "😐"
    }
    val timeString = try {
        val instant = java.time.Instant.ofEpochMilli(review.createdAt)
        val local = instant.atZone(java.time.ZoneId.systemDefault())
        local.format(java.time.format.DateTimeFormatter.ofPattern("M月d日 HH:mm"))
    } catch (_: Exception) {
        try {
            val date = LocalDate.parse(review.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            date.format(DateTimeFormatter.ofPattern("M月d日"))
        } catch (_: Exception) { review.date }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onToggle,
                onLongClick = onLongClick
            )
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emotionEmoji, fontSize = MaterialTheme.typography.titleLarge.fontSize)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        color = LightInk
                    )
                    Text(
                        text = review.reviewText.take(100) +
                            if (review.reviewText.length > 100) "..." else "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = LightInk.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(color = FlowerCyan.copy(alpha = 0.1f))
                    Spacer(Modifier.height(8.dp))

                    if (review.reviewText.length > 100) {
                        Text(
                            text = review.reviewText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    val analysisBlocks = buildList {
                        review.emotionDetail?.let { add("💭 情绪分析" to it) }
                        review.unfinishedReason?.let { add("🔍 原因分析" to it) }
                        review.expBalanceComment?.let { add("⚖️ 经验分配" to it) }
                        review.attributeBalanceComment?.let { add("📊 属性均衡" to it) }
                        review.tomorrowSuggestion?.let { add("💡 明日建议" to it) }
                        review.strategyAdvice?.let { add("🎯 策略建议" to it) }
                    }

                    if (analysisBlocks.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        analysisBlocks.forEach { (title, content) ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = FlowerCyan.copy(alpha = 0.05f)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = FlowerCyan
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!isExpanded) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "长按删除",
                    style = MaterialTheme.typography.labelSmall,
                    color = LightInk.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
fun EmotionChip(
    emoji: String,
    label: String,
    @Suppress("UNUSED_PARAMETER") value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) FlowerCyan.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) ButtonDefaults.outlinedButtonBorder else null
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = MaterialTheme.typography.headlineSmall.fontSize)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) FlowerCyan else LightInk
            )
        }
    }
}
